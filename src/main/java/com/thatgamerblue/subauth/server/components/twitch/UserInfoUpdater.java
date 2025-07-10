package com.thatgamerblue.subauth.server.components.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.common.exception.UnauthorizedException;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.domain.Subscription;
import com.github.twitch4j.helix.domain.SubscriptionList;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.thatgamerblue.subauth.server.components.event.EventBus;
import com.thatgamerblue.subauth.server.components.event.events.TwitchUserUpdated;
import com.thatgamerblue.subauth.server.database.twitch.TwitchUserEntity;
import com.thatgamerblue.subauth.server.database.twitch.TwitchUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
	}

	@Transactional
	@Scheduled(fixedDelay = BATCH_DELAY_SECONDS, timeUnit = TimeUnit.SECONDS)
	public void updateData() {
		Flux.fromIterable(twitchUserRepository.getLeastRecentlyUpdated(BATCH_SIZE))
			.flatMap(caster -> getSubscriptionsForBroadcaster(caster).collectList().map(list -> Tuples.of(caster, list)))
			.flatMap(tuple -> {
				TwitchUserEntity caster = tuple.getT1();
				List<Subscription> currentSubscribers = tuple.getT2();
				List<String> currentSubscriberIds = currentSubscribers.stream().map(Subscription::getUserId).toList();
				List<String> oldSubscribers = caster.getSubscribers();
				caster.setSubscribers(currentSubscriberIds);
				caster.setLastCheck(Instant.now());
				if (!currentSubscribers.isEmpty()) {
					Subscription first = currentSubscribers.getFirst();
					caster.setRecentlyKnownLogin(first.getBroadcasterLogin());
				}
				twitchUserRepository.save(caster);
				if (CollectionUtils.disjunction(currentSubscriberIds, oldSubscribers).isEmpty()) {
					return Mono.empty();
				} else {
					return Mono.just(caster);
				}
			})
			.doOnNext(caster -> eventBus.post(new TwitchUserUpdated(caster)))
			.onErrorResume(t -> {
				log.info("Error updating caster: ", t);
				return Mono.empty();
			})
			.subscribeOn(Schedulers.boundedElastic())
			.subscribe();
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
		return Mono.fromSupplier(s).onErrorResume(HystrixRuntimeException.class, t -> {
			if (t.getCause() instanceof UnauthorizedException) {
				return Mono.just(new OAuth2Credential(identityProvider.getProviderName(), caster.getAccessToken(), caster.getRefreshToken(), null, null, null, null))
					.map(cred -> identityProvider.refreshCredential(cred).get())
					.switchIfEmpty(markCasterFailed(caster).then(Mono.empty()))
					.map(cred -> {
						caster.setAccessToken(cred.getAccessToken());
						caster.setRefreshToken(cred.getRefreshToken());
						twitchUserRepository.save(caster);
						return caster;
					}).then(Mono.fromSupplier(s));
			}
			return markCasterFailed(caster).then(Mono.empty());
		});
	}

	private Mono<Void> markCasterFailed(TwitchUserEntity caster) {
		return Mono.fromRunnable(() -> {
			caster.setLastRefreshValid(false);
			twitchUserRepository.save(caster);
		});
	}
}
