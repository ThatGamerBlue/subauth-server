package com.thatgamerblue.subauth.server.util;

import com.google.gson.Gson;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {
	private final SecretKey verifySecret;
	private final JwtParser parser;
	private final Gson gson;

	private JwtUtils(Gson gson) throws DecoderException {
		byte[] verifySecretBytes = Hex.decodeHex(Env.Jwt.VERIFY_SECRET.get());
		if (verifySecretBytes.length < 64) {
			throw new RuntimeException("JWT Secret not long enough! Minimum 512 bits (64 bytes)");
		}
		this.verifySecret = Keys.hmacShaKeyFor(verifySecretBytes);
		this.gson = gson;
		this.parser = Jwts.parser().verifyWith(verifySecret).build();
	}

	public String createJwt(Object object) {
		return createJwt(object, object.getClass());
	}

	public String createJwt(Object object, Class<?> baseType) {
		JwtBuilder builder = Jwts.builder()
			.content(gson.toJson(object, baseType).getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_JSON.toString())
			.signWith(verifySecret, Jwts.SIG.HS512);

		return builder.compact();
	}

	public <T> T decodeJwt(String token, Class<T> clazz) {
		try {
			Jws<byte[]> parsed = parser.parseSignedContent(token);
			String value = new String(parsed.getPayload(), StandardCharsets.UTF_8);
			return gson.fromJson(value, clazz);
		} catch (Throwable ex) {
			return null;
		}
	}
}
