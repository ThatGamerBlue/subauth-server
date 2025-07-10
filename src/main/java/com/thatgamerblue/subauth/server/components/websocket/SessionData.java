package com.thatgamerblue.subauth.server.components.websocket;

import com.google.gson.Gson;
import com.thatgamerblue.subauth.server.components.websocket.messages.WSMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.Disposable;

@RequiredArgsConstructor
public class SessionData {
	@Delegate
	private final WebSocketSession session;
	private final Gson gson;

	private final List<Disposable> subscriptions = new ArrayList<>();
	@Getter
	private final Set<String> seenTokens = Collections.synchronizedSet(new HashSet<>());

	@Getter
	private AtomicBoolean authenticated = new AtomicBoolean(false);

	public void send(WSMessage message) throws Exception {
		String serialized = gson.toJson(message);
		session.sendMessage(new TextMessage(serialized));
	}

	public void addDisposable(Disposable d) {
		synchronized (subscriptions) {
			subscriptions.add(d);
		}
	}

	public void clearDisposables() {
		synchronized (subscriptions) {
			for (Disposable subscription : subscriptions) {
				subscription.dispose();
			}

			subscriptions.clear();
		}
	}
}
