package com.thatgamerblue.subauth.server.components.websocket.messages;

import lombok.Value;

@Value
public class AuthenticationMessage extends WSMessage {
	String token;
}
