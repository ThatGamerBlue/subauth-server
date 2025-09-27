package com.thatgamerblue.subauth.server.components.websocket.messages;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ErrorMessage extends WSMessage {
	ErrorType error;
	String extraData;

	public ErrorMessage(ErrorType error) {
		this(error, null);
	}

	public enum ErrorType {
		INVALID_MESSAGE, INVALID_TOKEN, NO_HANDLER, ALREADY_SUBSCRIBED, UNKNOWN_USER;
	}
}
