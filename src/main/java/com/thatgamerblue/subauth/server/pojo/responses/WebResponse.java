package com.thatgamerblue.subauth.server.pojo.responses;

import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public class WebResponse {
	String error;

	protected WebResponse(String error) {
		this.error = error;
	}

	public static WebResponse error(String error) {
		return new WebResponse(error);
	}
}
