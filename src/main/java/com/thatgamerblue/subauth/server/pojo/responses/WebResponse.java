package com.thatgamerblue.subauth.server.pojo.responses;

import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public class WebResponse {
	String error;
	String success;

	protected WebResponse(boolean isError, String message) {
		this.error = isError ? message : null;
		this.success = isError ? null : message;
	}

	protected WebResponse() {
		this(false, null);
	}

	public static WebResponse error(String error) {
		return new WebResponse(true, error);
	}

	public static WebResponse success(String success) {
		return new WebResponse(false, success);
	}
}
