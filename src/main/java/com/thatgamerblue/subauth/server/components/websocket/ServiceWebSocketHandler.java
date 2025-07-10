package com.thatgamerblue.subauth.server.components.websocket;

import com.thatgamerblue.subauth.server.pojo.subscriptions.Subscription;
import reactor.core.Disposable;

public interface ServiceWebSocketHandler<Sub extends Subscription> {
	Disposable startHandlingEvents(SessionData session, Sub subscription);

	void sendInitialMessage(SessionData session, Sub subscription) throws Exception;

	Class<Sub> getSubscriptionType();
}
