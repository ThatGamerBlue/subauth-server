package com.thatgamerblue.subauth.server.components.twitch;

import com.thatgamerblue.subauth.server.components.websocket.SubAuthWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class RegisterWebSocketRoute implements WebSocketConfigurer {
	private final SubAuthWebSocketHandler subAuthWebSocketHandler;

	public RegisterWebSocketRoute(SubAuthWebSocketHandler subAuthWebSocketHandler) {
		this.subAuthWebSocketHandler = subAuthWebSocketHandler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(subAuthWebSocketHandler, "/ws");
	}
}
