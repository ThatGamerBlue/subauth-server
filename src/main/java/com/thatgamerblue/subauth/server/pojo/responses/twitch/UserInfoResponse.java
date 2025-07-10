package com.thatgamerblue.subauth.server.pojo.responses.twitch;

import com.thatgamerblue.subauth.server.pojo.responses.WebResponse;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public class UserInfoResponse extends WebResponse {
	String login;
	String userId;

	UserInfoResponse(String login, String userId) {
		this.login = login;
		this.userId = userId;
	}

	public static UserInfoResponse of(String login, String userId) {
		return new UserInfoResponse(login, userId);
	}
}
