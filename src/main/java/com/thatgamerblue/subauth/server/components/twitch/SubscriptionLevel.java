package com.thatgamerblue.subauth.server.components.twitch;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SubscriptionLevel {
	TIER_1("1000", 1),
	TIER_2("2000", 2),
	TIER_3("3000", 3);
	
	private final String twitchTier;
	private final int level;

	public String toTier() {
		return twitchTier;
	}

	public boolean isValidForConstraint(SubscriptionLevel constraint) {
		return constraint.level <= level;
	}

	public static boolean isValidTier(String tier) {
		for (SubscriptionLevel level : values()) {
			if (level.twitchTier.equals(tier)) {
				return true;
			}
		}
		return false;
	}

	public static SubscriptionLevel fromTier(String tier) {
		for (SubscriptionLevel level : values()) {
			if (level.twitchTier.equals(tier)) {
				return level;
			}
		}
		return TIER_1;
	}

}
