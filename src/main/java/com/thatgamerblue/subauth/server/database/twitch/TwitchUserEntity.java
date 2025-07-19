package com.thatgamerblue.subauth.server.database.twitch;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

@Data
@Entity
@Table(name = "twitch_users")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TwitchUserEntity {
	@Id
	private String userId;

	private String recentlyKnownLogin;

	private String accessToken;

	private String refreshToken;

	private Instant lastCheck;

	@CreationTimestamp
	private Instant createdAt;

	private Instant tokensValidFrom;

	private boolean lastRefreshValid;

	private boolean canHaveSubscribers;

	@Column(nullable = true)
	private String minecraftUuid;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "twitch_subscriptions", joinColumns = @JoinColumn(name = "userId"))
	private List<String> subscribers;
}
