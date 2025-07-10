package com.thatgamerblue.subauth.server.pojo.twitch;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Value;

@Value
public class TwitchStateToken {
	@SerializedName("a")
	String uuid;
	@SerializedName("b")
	Instant timestamp;
	@SerializedName("c")
	String nonce;
}
