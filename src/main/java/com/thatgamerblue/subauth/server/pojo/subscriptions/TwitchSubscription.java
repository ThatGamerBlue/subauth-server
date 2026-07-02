package com.thatgamerblue.subauth.server.pojo.subscriptions;

import com.thatgamerblue.subauth.server.components.twitch.SubscriptionLevel;
import java.time.Instant;
import lombok.Value;

@Value
public class TwitchSubscription extends Subscription {
	String userId;
	SubscriptionLevel tier;
	Instant createdAt;

	public SubscriptionLevel getTier() {
		return tier == null ? SubscriptionLevel.TIER_1 : tier;
	}
}
