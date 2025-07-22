package com.thatgamerblue.subauth.server.components.event.events;

import com.thatgamerblue.subauth.server.database.twitch.TwitchUserEntity;
import lombok.Value;

@Value
public class TwitchUserLinkUpdated {
	/**
	 * When linking a new account, this is the users new UUID
	 * When unlinking an account, this is the users UUID before the unlinking takes place
	 */
	String uuid;
	TwitchUserEntity entity;
}
