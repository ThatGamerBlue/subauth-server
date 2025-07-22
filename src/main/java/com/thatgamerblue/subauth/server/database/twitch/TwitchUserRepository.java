package com.thatgamerblue.subauth.server.database.twitch;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface TwitchUserRepository extends JpaRepository<TwitchUserEntity, String> {
	@Query("select u from TwitchUserEntity u where u.lastRefreshValid=true and u.canHaveSubscribers=true and u.lastCheck < :olderThan order by u.lastCheck asc limit ?1")
	List<TwitchUserEntity> getLeastRecentlyUpdated(int count, Instant olderThan);

	@Modifying
	@Query("update TwitchUserEntity u set u.minecraftUuid=null where u.minecraftUuid=?1 and u.userId <> ?2")
	void updateUuidToNullForOtherUsers(String uuid, String userId);

	Optional<TwitchUserEntity> findFirstByMinecraftUuid(String uuid);

	List<TwitchUserEntity> getAllBySubscribersContaining(String userId);
}
