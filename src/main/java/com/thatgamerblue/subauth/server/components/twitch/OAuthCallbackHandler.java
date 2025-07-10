package com.thatgamerblue.subauth.server.components.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;
import com.thatgamerblue.subauth.server.components.event.EventBus;
import com.thatgamerblue.subauth.server.components.event.events.MinecraftUserChanged;
import com.thatgamerblue.subauth.server.database.twitch.TwitchUserEntity;
import com.thatgamerblue.subauth.server.database.twitch.TwitchUserRepository;
import com.thatgamerblue.subauth.server.pojo.twitch.TwitchStateToken;
import com.thatgamerblue.subauth.server.util.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/twitch")
@EnableScheduling
public class OAuthCallbackHandler {
	private static final Duration TOKEN_EXPIRY = Duration.of(10, ChronoUnit.MINUTES);
	private static final Duration CACHE_EXPIRY = TOKEN_EXPIRY.plus(10, ChronoUnit.MINUTES);
	private final List<TwitchStateToken> seenTokens = new CopyOnWriteArrayList<>();

	private final JwtUtils jwtUtils;
	private final TwitchIdentityProvider twitchIdentityProvider;
	private final TwitchHelix twitchHelix;
	private final TwitchUserRepository twitchUserRepository;
	private final EventBus eventBus;

	public OAuthCallbackHandler(JwtUtils jwtUtils, TwitchIdentityProvider twitchIdentityProvider, TwitchHelix twitchHelix, TwitchUserRepository twitchUserRepository, EventBus eventBus) {
		this.jwtUtils = jwtUtils;
		this.twitchIdentityProvider = twitchIdentityProvider;
		this.twitchHelix = twitchHelix;
		this.twitchUserRepository = twitchUserRepository;
		this.eventBus = eventBus;
	}

	@Transactional
	@GetMapping("/oauth_callback")
	public String oauthCallback(
		HttpServletResponse response,
		@RequestParam(value = "code", required = false) String code,
		@RequestParam(value = "state") String state,
		@RequestParam(value = "error", required = false) String error,
		@RequestParam(value = "error_description", required = false) String errorDescription
	) {
		if (error != null) {
			response.setStatus(400);
			return getErrorPage("Twitch", error, errorDescription);
		}
		TwitchStateToken token = jwtUtils.decodeJwt(state, TwitchStateToken.class);
		if (token == null || Instant.now().isAfter(token.getTimestamp().plus(TOKEN_EXPIRY)) || seenTokens.contains(token)) {
			return getErrorPage("Something", "invalid state", "state parameter was not valid");
		}
		try {
			handleGotCode(token, code);
			eventBus.post(new MinecraftUserChanged(token.getUuid()));
			return getSuccessPage();
		} catch (Exception ex) {
			ex.printStackTrace();
			return getErrorPage("Something", ex.getMessage(), "unknown cause");
		}
	}

	@Transactional
	protected void handleGotCode(TwitchStateToken token, String code) {
		OAuth2Credential cred = twitchIdentityProvider.getCredentialByCode(code);
		UserList list = twitchHelix.getUsers(cred.getAccessToken(), null, null).execute();
		User helixUser = list.getUsers().getFirst();

		twitchUserRepository.updateUuidToNullForOtherUsers(token.getUuid(), helixUser.getId());

		Optional<TwitchUserEntity> userOptional = twitchUserRepository.findById(helixUser.getId());
		TwitchUserEntity user = userOptional.orElse(new TwitchUserEntity());
		user.setUserId(helixUser.getId());
		user.setRecentlyKnownLogin(helixUser.getLogin());
		user.setAccessToken(cred.getAccessToken());
		user.setRefreshToken(cred.getRefreshToken());
		user.setLastRefreshValid(true);
		user.setMinecraftUuid(token.getUuid());
		user.setLastCheck(Instant.EPOCH);
		twitchUserRepository.save(user);
	}

	private String getErrorPage(String service, String error, String errorDescription) {
		return "<h2>Error</h2><p>" + service + " returned an error: '" + HtmlUtils.htmlEscape(error) + "' with description: '" + HtmlUtils.htmlEscape(errorDescription) + "'</p><p>Please return to the server, relog, and try again</p>";
	}

	private String getSuccessPage() {
		return "<h2>Success!</h2><p>You may now close this page, and return to the game.</p>";
	}

	@Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
	public void evictOldTokens() {
		seenTokens.removeIf(t -> Instant.now().isAfter(t.getTimestamp().plus(CACHE_EXPIRY)));
	}
}
