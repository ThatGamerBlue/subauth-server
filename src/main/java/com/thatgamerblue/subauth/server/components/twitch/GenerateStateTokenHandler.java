package com.thatgamerblue.subauth.server.components.twitch;

import com.thatgamerblue.subauth.server.pojo.responses.TokenAndRedirectResponse;
import com.thatgamerblue.subauth.server.pojo.responses.WebResponse;
import com.thatgamerblue.subauth.server.pojo.twitch.TwitchStateToken;
import com.thatgamerblue.subauth.server.util.Env;
import com.thatgamerblue.subauth.server.util.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/twitch")
public class GenerateStateTokenHandler {

	private final JwtUtils jwtUtils;

	public GenerateStateTokenHandler(JwtUtils jwtUtils) {
		this.jwtUtils = jwtUtils;
	}

	@GetMapping("/generate_state_token")
	public WebResponse generateStateToken(
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
		String randomValue = RandomStringUtils.randomAlphanumeric(16);
		TwitchStateToken stateToken = new TwitchStateToken(mcUuid, Instant.now(), randomValue);
		String jwtToken = jwtUtils.createJwt(stateToken);
		String redirect = new StringBuilder("https://id.twitch.tv/oauth2/authorize?")
			.append("client_id=").append(Env.Twitch.CLIENT_ID.get())
			.append("&redirect_uri=").append(URLEncoder.encode(Env.Twitch.OAUTH_REDIRECT.get(), StandardCharsets.UTF_8))
			.append("&state=").append(jwtToken)
			.append("&response_type=code")
			.append("&scope=channel:read:subscriptions")
			.toString();
		return TokenAndRedirectResponse.of(jwtUtils.createJwt(stateToken), redirect);
	}
}
