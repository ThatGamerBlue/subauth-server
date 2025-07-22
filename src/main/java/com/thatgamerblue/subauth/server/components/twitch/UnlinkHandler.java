package com.thatgamerblue.subauth.server.components.twitch;

import com.thatgamerblue.subauth.server.components.event.EventBus;
import com.thatgamerblue.subauth.server.components.event.events.TwitchUserLinkUpdated;
import com.thatgamerblue.subauth.server.database.twitch.TwitchUserEntity;
import com.thatgamerblue.subauth.server.database.twitch.TwitchUserRepository;
import com.thatgamerblue.subauth.server.pojo.responses.WebResponse;
import com.thatgamerblue.subauth.server.util.Env;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/twitch")
public class UnlinkHandler {
	private final TwitchUserRepository twitchUserRepository;
	private final EventBus eventBus;

	public UnlinkHandler(TwitchUserRepository twitchUserRepository, EventBus eventBus) {
		this.twitchUserRepository = twitchUserRepository;
		this.eventBus = eventBus;
	}

	@GetMapping("/unlink")
	public WebResponse unlink(
		HttpServletResponse response,
		@RequestParam(value = "token", required = false) String token,
		@RequestParam(value = "mcUuid", required = false) String mcUuid
	) {
		if (token == null) {
			response.setStatus(400);
			return WebResponse.error("missing token");
		}
		if (mcUuid == null) {
			response.setStatus(400);
			return WebResponse.error("missing mcUuid");
		}

		String expectedToken = Env.TOKEN_PSK.get();
		if (expectedToken == null || expectedToken.trim().isEmpty() || !expectedToken.equals(token)) {
			response.setStatus(403);
			return WebResponse.error("invalid authorization");
		}

		Optional<TwitchUserEntity> optionalEntity = twitchUserRepository.findFirstByMinecraftUuid(mcUuid);
		if (optionalEntity.isEmpty()) {
			response.setStatus(404);
			return WebResponse.error("invalid mcUuid");
		}
		TwitchUserEntity entity = optionalEntity.get();
		entity.setMinecraftUuid(null);
		entity.setTokensValidFrom(Instant.now());
		twitchUserRepository.save(entity);

		eventBus.post(new TwitchUserLinkUpdated(mcUuid, entity));

		return WebResponse.success("unlinked");
	}
}
