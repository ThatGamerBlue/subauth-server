package com.thatgamerblue.subauth.server.external.twitch;

import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import com.thatgamerblue.subauth.server.util.Env;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class TwitchConfigurator {
	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
	public TwitchHelix buildHelix() {
		return TwitchHelixBuilder.builder()
			.withClientId(Env.Twitch.CLIENT_ID.get())
			.withClientSecret(Env.Twitch.CLIENT_SECRET.get())
			.build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
	public TwitchIdentityProvider buildIdentityProvider() {
		return new TwitchIdentityProvider(Env.Twitch.CLIENT_ID.get(), Env.Twitch.CLIENT_SECRET.get(), Env.Twitch.OAUTH_REDIRECT.get());
	}
}
