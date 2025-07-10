package com.thatgamerblue.subauth.server.pojo;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapterFactory;
import com.thatgamerblue.subauth.server.pojo.subscriptions.Subscription;
import com.thatgamerblue.subauth.server.pojo.subscriptions.TwitchSubscription;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GsonConfigurator {
	@Bean
	public GsonBuilderCustomizer customizer(List<TypeAdapterFactory> adapterFactories) {
		return (builder) -> {
			builder.disableHtmlEscaping().registerTypeAdapter(Instant.class, new InstantSerializer());
			adapterFactories.forEach(builder::registerTypeAdapterFactory);
		};
	}

	private static class InstantSerializer implements JsonSerializer<Instant>, JsonDeserializer<Instant> {

		@Override
		public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return Instant.parse(json.getAsString());
		}

		@Override
		public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(src.toString());
		}
	}
}
