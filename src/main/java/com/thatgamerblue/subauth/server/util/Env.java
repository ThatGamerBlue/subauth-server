package com.thatgamerblue.subauth.server.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Env<T> {
	public static class Database {
		public static final Env<String> URL = Env.of("DATABASE_URL");
		public static final Env<String> USER = Env.of("DATABASE_USER");
		public static final Env<String> PASSWORD = Env.of("DATABASE_PASSWORD");
	}

	public static class Twitch {
		public static final Env<String> CLIENT_ID = Env.of("TWITCH_CLIENT_ID");
		public static final Env<String> CLIENT_SECRET = Env.of("TWITCH_CLIENT_SECRET");
		public static final Env<String> OAUTH_REDIRECT = Env.of("TWITCH_OAUTH_REDIRECT");
	}

	public static class Jwt {
		public static final Env<String> VERIFY_SECRET = Env.of("JWT_VERIFY_SECRET");
	}

	public static final Env<String> TOKEN_PSK = Env.of("TOKEN_PSK");
	public static final Env<String> MINECRAFT_SERVER = Env.of("MINECRAFT_SERVER");

	private static final Map<String, String> envMap;

	private final String key;
	private final Function<String, T> mapper;

	public String getString(String def) {
		String mapVal = envMap.get(this.key);
		if (mapVal != null) {
			return mapVal;
		}
		String sysVal = System.getenv(this.key);
		return sysVal != null ? sysVal : def;
	}

	public String getString() {
		return getString(null);
	}

	public T get(T def) {
		String value = getString(null);
		if (value == null) {
			return def;
		} else {
			return mapper.apply(value);
		}
	}

	public T get() {
		return get(null);
	}

	private static Env<String> of(String env) {
		return new Env<>(env, Function.identity());
	}

	private static <T> Env<T> of(String env, Function<String, T> mapper) {
		return new Env<>(env, mapper);
	}

	static {
		final String envEnv = System.getenv("ENV_FILE");
		final File envFile = new File(envEnv == null ? "env.json" : envEnv);
		Map<String, String> tmpMap;
		try (FileReader fr = new FileReader(envFile)) {
			// @formatter:off
			tmpMap = new Gson().fromJson(fr, new TypeToken<Map<String, String>>(){}.getType());
			// @formatter:on
		} catch (IOException e) {
			if (envEnv != null || envFile.exists()) {
				log.error("Failed to load environment file {}, not using it", envFile.getAbsolutePath(), e);
			} else {
				log.error("Failed to load default environment file, not using it. {}", e.getMessage());
			}
			tmpMap = new HashMap<>(); // use empty map to simplify code
		}
		envMap = tmpMap;
	}
}
