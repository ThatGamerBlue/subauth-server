package com.thatgamerblue.subauth.server.components.event.events;

import com.thatgamerblue.subauth.server.database.twitch.TwitchUserEntity;
import lombok.Value;

@Value
public class TwitchUserUpdated {
	TwitchUserEntity entity;
}
