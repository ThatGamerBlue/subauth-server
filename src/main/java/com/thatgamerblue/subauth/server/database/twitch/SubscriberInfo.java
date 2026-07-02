package com.thatgamerblue.subauth.server.database.twitch;

import com.thatgamerblue.subauth.server.components.twitch.SubscriptionLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriberInfo {
	@Column(name = "subscriber_id", nullable = false)
	private String userId;

	@Column(name = "tier", nullable = false)
	@Enumerated(EnumType.STRING)
	@ColumnDefault("'TIER_1'")
	private SubscriptionLevel tier = SubscriptionLevel.TIER_1;
}
