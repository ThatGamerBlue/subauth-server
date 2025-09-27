package com.thatgamerblue.subauth.server.components.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.domain.Subscription;
import com.github.twitch4j.helix.domain.SubscriptionList;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;
import com.google.common.base.Strings;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.thatgamerblue.subauth.server.components.event.EventBus;
import com.thatgamerblue.subauth.server.components.event.events.TwitchUserLinkUpdated;
import com.thatgamerblue.subauth.server.components.event.events.TwitchSubscriberListUpdated;
import com.thatgamerblue.subauth.server.database.twitch.TwitchUserEntity;
import com.thatgamerblue.subauth.server.database.twitch.TwitchUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

@Slf4j
@Component
@EnableScheduling
public class UserInfoUpdater {
	private static final int BATCH_DELAY_SECONDS = 5;
	private static final int BATCH_SIZE = 50;

	private final EventBus eventBus;
	private final TwitchUserRepository twitchUserRepository;
	private final TwitchIdentityProvider identityProvider;
	private final TwitchHelix helix;

	public UserInfoUpdater(EventBus eventBus, TwitchUserRepository twitchUserRepository, TwitchIdentityProvider identityProvider, TwitchHelix helix) {
		this.eventBus = eventBus;
		this.twitchUserRepository = twitchUserRepository;
		this.identityProvider = identityProvider;
		this.helix = helix;

		eventBus.onEvent(TwitchUserLinkUpdated.class)
			.subscribe(this::onMinecraftUserUpdated);
	}

	private void onMinecraftUserUpdated(TwitchUserLinkUpdated event) {
		try {
			TwitchUserEntity updatedUser = event.getEntity();
			String userId = updatedUser.getUserId();
			List<TwitchUserEntity> casters = twitchUserRepository.getAllBySubscribersContaining(userId);
			casters.forEach(caster -> eventBus.post(new TwitchSubscriberListUpdated(caster)));
		} catch (Throwable ignored) {

		}
	}

	@Transactional
	@Scheduled(fixedDelay = BATCH_DELAY_SECONDS, timeUnit = TimeUnit.SECONDS)
	public void updateData() {
		Flux.fromIterable(twitchUserRepository.getLeastRecentlyUpdated(BATCH_SIZE, Instant.now().minus(1, ChronoUnit.MINUTES)))
			.flatMap(this::updateCasterData)
			.subscribeOn(Schedulers.boundedElastic())
			.subscribe();
	}

	private Mono<Void> updateCasterData(TwitchUserEntity casterIn) {
		return Mono.just(casterIn)
			.flatMap(this::updateTwitchUserLoginName)
			.flatMap(caster -> getSubscriptionsForBroadcaster(caster).collectList().map(list -> Tuples.of(caster, list)))
			.flatMap(tuple -> {
				TwitchUserEntity caster = tuple.getT1();
				List<Subscription> currentSubscribers = tuple.getT2();
				List<String> currentSubscriberIds = currentSubscribers.stream().map(Subscription::getUserId).toList();
				List<String> oldSubscribers = caster.getSubscribers();
				caster.setSubscribers(currentSubscriberIds);
				caster.setLastCheck(Instant.now());
				twitchUserRepository.save(caster);
				if (CollectionUtils.disjunction(currentSubscriberIds, oldSubscribers).isEmpty()) {
					return Mono.empty();
				} else {
					return Mono.just(caster);
				}
			})
			.doOnNext(caster -> eventBus.post(new TwitchSubscriberListUpdated(caster)))
			.onErrorResume(HystrixRuntimeException.class, ex -> markCasterFailed(ex, casterIn).then(Mono.empty()))
			.onErrorResume(t -> {
				log.info("Error updating caster: ", t);
				return Mono.empty();
			}).then();
	}

	private Mono<TwitchUserEntity> updateTwitchUserLoginName(TwitchUserEntity entity) {
		if (entity.getLastCheck().isAfter(Instant.now().minus(1, ChronoUnit.HOURS))) {
			return Mono.just(entity);
		}

		Supplier<UserList> s = () -> helix.getUsers(entity.getAccessToken(), null, null).execute();
		return Mono.fromSupplier(s)
			.onErrorResume(HystrixRuntimeException.class, t -> handleHystrixRuntimeError(t, entity, s))
			.doOnNext(userList -> {
				User user = userList.getUsers().getFirst();
				entity.setCanHaveSubscribers(!Strings.isNullOrEmpty(user.getBroadcasterType()));
				entity.setRecentlyKnownLogin(user.getLogin());
				twitchUserRepository.save(entity);
			}).map(a -> entity);
	}

	private Flux<Subscription> getSubscriptionsForBroadcaster(TwitchUserEntity caster) {
		// https://stackoverflow.com/a/67131903
		return getNextSubscriptionPage(caster, null)
			.expand(list -> {
				if (list.getPagination() == null || list.getPagination().getCursor() == null || list.getPagination().getCursor().isEmpty()) {
					return Mono.empty();
				} else {
					return getNextSubscriptionPage(caster, list.getPagination().getCursor());
				}
			})
			.flatMapIterable(SubscriptionList::getSubscriptions);
	}

	private Mono<SubscriptionList> getNextSubscriptionPage(TwitchUserEntity caster, String cursor) {
		Supplier<SubscriptionList> s = () -> helix.getSubscriptions(caster.getAccessToken(), caster.getUserId(), cursor, null, 100).execute();
		return Mono.fromSupplier(s)
			.onErrorResume(HystrixRuntimeException.class, t -> handleHystrixRuntimeError(t, caster, s));
	}

	private <T> Mono<T> handleHystrixRuntimeError(HystrixRuntimeException ex, TwitchUserEntity caster, Supplier<T> supplier) {
		return Mono.just(new OAuth2Credential(identityProvider.getProviderName(), caster.getAccessToken(), caster.getRefreshToken(), null, null, null, null))
			.delayElement(Duration.of(5, ChronoUnit.SECONDS)) // wait 5 seconds for rate limiting before updating token
			.map(identityProvider::refreshCredential)
			.flatMap(o -> o.map(Mono::just).orElseGet(Mono::empty))
			.switchIfEmpty(markCasterFailed(ex, caster).then(Mono.empty()))
			.map(cred -> {
				caster.setAccessToken(cred.getAccessToken());
				caster.setRefreshToken(cred.getRefreshToken());
				twitchUserRepository.save(caster);
				return caster;
			})
			.then(Mono.fromSupplier(supplier));
	}

	private Mono<Void> markCasterFailed(Throwable ex, TwitchUserEntity caster) {
		Throwable cause = ex.getCause();
		if (cause instanceof TimeoutException) {
			return Mono.fromRunnable(() -> {
				log.info("Timeout checking caster {} ({}), backing off.", caster.getUserId(), caster.getRecentlyKnownLogin());
				caster.setLastCheck(Instant.now().plus(10, ChronoUnit.MINUTES));
				twitchUserRepository.save(caster);
			});
		}
		return Mono.fromRunnable(() -> {
			log.info("Marking caster {} as failed due to exception", caster.getRecentlyKnownLogin(), ex);
			caster.setLastRefreshValid(false);
			twitchUserRepository.save(caster);
		});
	}
}
