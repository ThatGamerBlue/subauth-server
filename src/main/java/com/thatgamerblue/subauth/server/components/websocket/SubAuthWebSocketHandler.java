package com.thatgamerblue.subauth.server.components.websocket;
import com.google.gson.Gson;
import com.thatgamerblue.subauth.server.components.websocket.messages.AuthenticationMessage;
import com.thatgamerblue.subauth.server.components.websocket.messages.ErrorMessage;
import com.thatgamerblue.subauth.server.components.websocket.messages.ErrorMessage.ErrorType;
import com.thatgamerblue.subauth.server.components.websocket.messages.WSMessage;
import com.thatgamerblue.subauth.server.pojo.subscriptions.Subscription;
import com.thatgamerblue.subauth.server.util.JwtUtils;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class SubAuthWebSocketHandler extends TextWebSocketHandler {
	// close websocket connection if client doesn't authenticate within x time
	private static final Duration AUTHENTICATE_TIME = Duration.of(10, ChronoUnit.SECONDS);

	private final Map<String, SessionData> threadSafeSessions = new ConcurrentHashMap<>();

	private final JwtUtils jwtUtils;
	private final Gson gson;
	private final Map<Class<? super Subscription>, ServiceWebSocketHandler<? super Subscription>> handlerMap;

	public SubAuthWebSocketHandler(JwtUtils jwtUtils, Gson gson, Map<Class<? super Subscription>, ServiceWebSocketHandler<? super Subscription>> handlerMap) {
		this.jwtUtils = jwtUtils;
		this.gson = gson;
		this.handlerMap = handlerMap;
	}

	@Override
	protected void handleTextMessage(WebSocketSession _session, TextMessage _message) throws Exception {
		SessionData session = threadSafeSessions.get(_session.getId());
		String payload = _message.getPayload();
		WSMessage message = gson.fromJson(payload, WSMessage.class);

		if (message instanceof AuthenticationMessage auth) {
			handleAuthenticationMessage(session, auth);
		}
	}

	private void handleAuthenticationMessage(SessionData session, AuthenticationMessage message) throws Exception {
		String token = message.getToken();
		Subscription sub = jwtUtils.decodeJwt(token, Subscription.class);
		if (sub == null) {
			session.send(new ErrorMessage(ErrorType.INVALID_TOKEN));
			return;
		}
		session.getAuthenticated().set(true);

		ServiceWebSocketHandler<? super Subscription> handler = handlerMap.get(sub.getClass());
		if (handler == null) {
			log.info("unknown handler type: " + sub.getClass().getName());
			log.info("valid types:");
			handlerMap.entrySet().forEach(entry -> log.info("{}", entry));
			session.send(new ErrorMessage(ErrorType.NO_HANDLER));
			session.close();
			return;
		}

		if (session.getSeenTokens().contains(token)) {
			session.send(new ErrorMessage(ErrorType.ALREADY_SUBSCRIBED));
			// don't close, just do nothing
			return;
		}
		session.getSeenTokens().add(token);
		session.addDisposable(handler.startHandlingEvents(session, sub));
		handler.sendInitialMessage(session, sub);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession _session) throws Exception {
		SessionData session = new SessionData(new ConcurrentWebSocketSessionDecorator(_session, 2000, 4 * 1024), gson);
		threadSafeSessions.put(session.getId(), session);
		Mono.just(session)
			.delayElement(AUTHENTICATE_TIME)
			.flatMap(s -> s.getAuthenticated().get() ? Mono.empty() : Mono.just(s))
			.doOnNext(s -> {
				try {
					s.close(new CloseStatus(3003));
				} catch (IOException e) {
					// ignore
				}
			})
			.subscribeOn(Schedulers.boundedElastic())
			.subscribe();
	}

	@Override
	public void afterConnectionClosed(WebSocketSession _session, CloseStatus status) throws Exception {
		SessionData session = threadSafeSessions.get(_session.getId());
		threadSafeSessions.remove(session.getId());
		session.clearDisposables();
	}
}
