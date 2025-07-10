package com.thatgamerblue.subauth.server.components.websocket;

import com.thatgamerblue.subauth.server.components.websocket.SubAuthWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class RegisterWebSocketRoute implements WebSocketConfigurer {
	private final SubAuthWebSocketHandler subAuthWebSocketHandler;
	private final BackendWebSocketHandler backendWebSocketHandler;

	public RegisterWebSocketRoute(SubAuthWebSocketHandler subAuthWebSocketHandler, BackendWebSocketHandler backendWebSocketHandler) {
		this.subAuthWebSocketHandler = subAuthWebSocketHandler;
		this.backendWebSocketHandler = backendWebSocketHandler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(subAuthWebSocketHandler, "/ws");
		registry.addHandler(backendWebSocketHandler, "/backendws");
	}
}
