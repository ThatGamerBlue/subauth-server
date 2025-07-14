package com.thatgamerblue.subauth.server.components.websocket;

import com.google.common.base.Functions;
import com.google.gson.TypeAdapterFactory;
import com.thatgamerblue.subauth.server.components.websocket.messages.AuthenticationMessage;
import com.thatgamerblue.subauth.server.components.websocket.messages.ErrorMessage;
import com.thatgamerblue.subauth.server.components.websocket.messages.WSMessage;
import com.thatgamerblue.subauth.server.components.websocket.messages.WhitelistUpdateMessage;
import com.thatgamerblue.subauth.server.pojo.GsonTypeAdapters;
import com.thatgamerblue.subauth.server.pojo.subscriptions.Subscription;
import com.thatgamerblue.subauth.server.pojo.subscriptions.TwitchSubscription;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class WebSocketConfigurator {
	@Bean
	public TypeAdapterFactory adapterForWSMessage() {
		return GsonTypeAdapters.createFactory(WSMessage.class, Set.of(
			AuthenticationMessage.class,
			ErrorMessage.class,
			WhitelistUpdateMessage.class
		));
	}

	@Bean
	public TypeAdapterFactory adapterForSubscriptions() {
		return GsonTypeAdapters.createFactory(Subscription.class, Set.of(
			TwitchSubscription.class
		));
	}

	@Bean
	public Map<Class<? super Subscription>, ServiceWebSocketHandler<? super Subscription>> getHandlerMap(List<ServiceWebSocketHandler<?>> wsHandlers) {
		// ugly cast otherwise spring doesnt fill in the list
		return wsHandlers.stream().collect(Collectors.toMap(s -> ((ServiceWebSocketHandler<? super Subscription>) s).getSubscriptionType(), s -> (ServiceWebSocketHandler<? super Subscription>) s));
	}
}
