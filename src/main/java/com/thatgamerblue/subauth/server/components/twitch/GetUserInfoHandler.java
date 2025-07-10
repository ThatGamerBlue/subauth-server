package com.thatgamerblue.subauth.server.components.twitch;

import com.thatgamerblue.subauth.server.database.twitch.TwitchUserEntity;
import com.thatgamerblue.subauth.server.database.twitch.TwitchUserRepository;
import com.thatgamerblue.subauth.server.pojo.responses.WebResponse;
import com.thatgamerblue.subauth.server.pojo.responses.twitch.UserInfoResponse;
import com.thatgamerblue.subauth.server.util.Env;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/twitch")
public class GetUserInfoHandler {

	private final TwitchUserRepository twitchUserRepository;

	public GetUserInfoHandler(TwitchUserRepository twitchUserRepository) {
		this.twitchUserRepository = twitchUserRepository;
	}

	@GetMapping("/get_user_info")
	public WebResponse getUserInfo(
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

		Optional<TwitchUserEntity> optionalUser = twitchUserRepository.findFirstByMinecraftUuid(mcUuid);
		if (optionalUser.isEmpty()) {
			response.setStatus(404);
			return WebResponse.error("user not found");
		}

		TwitchUserEntity user = optionalUser.get();
		return UserInfoResponse.of(user.getRecentlyKnownLogin(), user.getUserId());
	}
}
