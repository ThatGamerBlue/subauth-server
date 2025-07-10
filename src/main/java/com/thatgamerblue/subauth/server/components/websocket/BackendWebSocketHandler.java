package com.thatgamerblue.subauth.server.components.websocket;

import com.thatgamerblue.subauth.server.components.event.EventBus;
import com.thatgamerblue.subauth.server.components.event.events.MinecraftUserChanged;
import com.thatgamerblue.subauth.server.util.Env;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class BackendWebSocketHandler extends TextWebSocketHandler {
	private WebSocketSession activeSession;
	private Disposable disposable;

	private final EventBus eventBus;

	public BackendWebSocketHandler(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		if (message.getPayload().equals(Env.TOKEN_PSK.get())) {
			activeSession = new ConcurrentWebSocketSessionDecorator(session, 2000, 4 * 1024);

			disposable = eventBus.onEvent(MinecraftUserChanged.class)
				.flatMap(mcUser -> Mono.fromRunnable(
							() -> {
								try {
									activeSession.sendMessage(new TextMessage(mcUser.getUuid()));
								} catch (IOException e) {
									e.printStackTrace();
									throw new RuntimeException(e);
								}
							}
						)
						.onErrorResume(t -> Mono.empty())
				)
				.onErrorResume(t -> Mono.empty())
				.subscribe();
		} else {
			session.close();
		}
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		if (activeSession != null) {
			session.close();
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		if (activeSession != null && session.getId().equals(activeSession.getId())) {
			disposable.dispose();
			disposable = null;
			activeSession = null;
		}
	}
}
