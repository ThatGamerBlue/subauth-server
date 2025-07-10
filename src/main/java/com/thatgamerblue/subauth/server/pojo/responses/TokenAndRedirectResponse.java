package com.thatgamerblue.subauth.server.pojo.responses;

import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public class TokenAndRedirectResponse extends TokenResponse {
	String redirect;

	TokenAndRedirectResponse(String token, String redirect) {
		super(token);
		this.redirect = redirect;
	}

	public static TokenAndRedirectResponse of(String token, String redirect) {
		return new TokenAndRedirectResponse(token, redirect);
	}
}
