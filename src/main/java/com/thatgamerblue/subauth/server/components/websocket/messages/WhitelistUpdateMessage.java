package com.thatgamerblue.subauth.server.components.websocket.messages;

import com.thatgamerblue.subauth.server.pojo.subscriptions.Subscription;
import java.util.List;
import lombok.Value;

@Value
public class WhitelistUpdateMessage extends WSMessage {
	Subscription cause;
	List<String> whitelist;
}
