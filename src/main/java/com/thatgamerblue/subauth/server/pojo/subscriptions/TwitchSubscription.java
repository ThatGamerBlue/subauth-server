package com.thatgamerblue.subauth.server.pojo.subscriptions;

import java.time.Instant;
import lombok.Value;

@Value
public class TwitchSubscription extends Subscription {
	String userId;
	Instant createdAt;
}
