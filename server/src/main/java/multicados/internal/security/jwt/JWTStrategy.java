/**
 * 
 */
package multicados.internal.security.jwt;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import org.springframework.core.env.Environment;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
class JWTStrategy {

	private final String secret;
	private final ZoneId zone;
	private final Duration expirationDuration;

	JWTStrategy(String secret, ZoneId zone, Duration expirationDuration) {
		this.secret = secret;
		this.zone = zone;
		this.expirationDuration = expirationDuration;
	}

	public Claims extractAllClaims(String token) {
		return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
	}

	public String createToken(Map<String, Object> claims, String username) {
		// @formatter:off
		return Jwts
				.builder()
				.setClaims(claims)
				.setSubject(username)
				.setIssuedAt(Date.from(ZonedDateTime.now().toInstant()))
				.setExpiration(Date.from(LocalDateTime.now()
						.plus(expirationDuration)
						.atZone(zone)
						.toInstant()))
				.signWith(SignatureAlgorithm.HS512, secret)
				.compact();
		// @formatter:on
	}

	public ZoneId getZone() {
		return zone;
	}

	public Duration getExpirationDuration() {
		return expirationDuration;
	}

	<T> T getOrDefault(Environment env, String propName, Function<String, T> producer, T defaultValue) {
		String configuredValue = env.getProperty(propName);
	
		if (!StringHelper.hasLength(configuredValue)) {
			return defaultValue;
		}
	
		return producer.apply(configuredValue);
	}

}
