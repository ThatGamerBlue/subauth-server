package com.thatgamerblue.subauth.server.components.twitch;

import com.thatgamerblue.subauth.server.database.twitch.TwitchUserEntity;
import com.thatgamerblue.subauth.server.database.twitch.TwitchUserRepository;
import com.thatgamerblue.subauth.server.pojo.responses.TokenResponse;
import com.thatgamerblue.subauth.server.pojo.responses.WebResponse;
import com.thatgamerblue.subauth.server.pojo.subscriptions.Subscription;
import com.thatgamerblue.subauth.server.pojo.subscriptions.TwitchSubscription;
import com.thatgamerblue.subauth.server.util.Env;
import com.thatgamerblue.subauth.server.util.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/twitch")
public class GenerateSubscribeTokenHandler {

	private final JwtUtils jwtUtils;
	private final TwitchUserRepository twitchUserRepository;

	public GenerateSubscribeTokenHandler(JwtUtils jwtUtils, TwitchUserRepository twitchUserRepository) {
		this.jwtUtils = jwtUtils;
		this.twitchUserRepository = twitchUserRepository;
	}

	@GetMapping("/generate_subscribe_token")
	public WebResponse generateSubscribeToken(
		HttpServletResponse response,
		@RequestParam(value = "token", required = false) String token,
		@RequestParam(value = "tier", required = false) String tier,
		@RequestParam(value = "mcUuid", required = false) String mcUuid
	) {
		if (token == null) {
			response.setStatus(400);
			return WebResponse.error("missing token");
		}
		if (tier == null || !SubscriptionLevel.isValidTier(tier)) {
			response.setStatus(400);
			return WebResponse.error("missing tier");
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

		Optional<TwitchUserEntity> entity = twitchUserRepository.findFirstByMinecraftUuid(mcUuid);
		if (entity.isEmpty()) {
			response.setStatus(404);
			return WebResponse.error("invalid mcUuid");
		}

		TwitchSubscription subscription = new TwitchSubscription(entity.get().getUserId(), SubscriptionLevel.fromTier(tier), Instant.now());

		return TokenResponse.of(jwtUtils.createJwt(subscription, Subscription.class));
	}
}
