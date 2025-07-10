package com.thatgamerblue.subauth.server.components.twitch;

import com.thatgamerblue.subauth.server.components.event.EventBus;
import com.thatgamerblue.subauth.server.components.event.events.TwitchUserUpdated;
import com.thatgamerblue.subauth.server.components.websocket.ServiceWebSocketHandler;
import com.thatgamerblue.subauth.server.components.websocket.SessionData;
import com.thatgamerblue.subauth.server.components.websocket.messages.ErrorMessage;
import com.thatgamerblue.subauth.server.components.websocket.messages.ErrorMessage.ErrorType;
import com.thatgamerblue.subauth.server.components.websocket.messages.WhitelistUpdateMessage;
import com.thatgamerblue.subauth.server.database.twitch.TwitchUserEntity;
import com.thatgamerblue.subauth.server.database.twitch.TwitchUserRepository;
import com.thatgamerblue.subauth.server.pojo.subscriptions.TwitchSubscription;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Component
public class TwitchWebSocketHandler implements ServiceWebSocketHandler<TwitchSubscription> {

	private final EventBus eventBus;
	private final TwitchUserRepository twitchUserRepository;

	public TwitchWebSocketHandler(EventBus eventBus, TwitchUserRepository twitchUserRepository) {
		this.eventBus = eventBus;
		this.twitchUserRepository = twitchUserRepository;
	}

	@Override
	public Disposable startHandlingEvents(SessionData session, TwitchSubscription subscription) {
		return eventBus.onEvent(TwitchUserUpdated.class)
			.map(TwitchUserUpdated::getEntity)
			.filter(entity -> entity.getUserId().equals(subscription.getUserId()))
			.flatMap(entity -> Mono.fromRunnable(
						() -> onUserUpdated(session, subscription, entity)
					)
					.onErrorResume(t -> Mono.empty())
			)
			.subscribe();
	}

	@Override
	public void sendInitialMessage(SessionData session, TwitchSubscription subscription) throws Exception {
		Optional<TwitchUserEntity> user = twitchUserRepository.findById(String.valueOf(subscription.getUserId()));
		if (user.isEmpty() || user.get().getMinecraftUuid() == null || !user.get().isLastRefreshValid()) {
			session.send(new ErrorMessage(ErrorType.UNKNOWN_USER));
			session.close();
			return;
		}

		onUserUpdated(session, subscription, user.get());
	}

	@Override
	public Class<TwitchSubscription> getSubscriptionType() {
		return TwitchSubscription.class;
	}

	@SneakyThrows
	public void onUserUpdated(SessionData session, TwitchSubscription subscription, TwitchUserEntity entity) {
		List<TwitchUserEntity> subscribers = twitchUserRepository.findAllById(entity.getSubscribers());
		List<String> minecraftUuids = subscribers.stream().map(TwitchUserEntity::getMinecraftUuid).filter(Objects::nonNull).toList();
		session.send(new WhitelistUpdateMessage(subscription, minecraftUuids));
	}
}
