/**
 * 
 */
package multicados.internal.security.jwt;

import static multicados.internal.helper.Utils.declare;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.servlet.http.Cookie;

import org.springframework.core.env.Environment;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils;
import multicados.internal.security.DomainUserDetails;

/**
 * @author Ngoc Huy
 *
 */
class JWTStrategy {

	private final JWTSecurityContext jwtSecurityContext;

	private final String secret;
	private final ZoneId zone;
	private final Duration expirationDuration;
	private final int maxAge;

	private final Cookie logoutCookie;

	JWTStrategy(JWTSecurityContext jwtSecurityContext, String secret, ZoneId zone, Duration expirationDuration)
			throws Exception {
		this.jwtSecurityContext = jwtSecurityContext;
		this.secret = secret;
		this.zone = zone;
		this.expirationDuration = expirationDuration;
		maxAge = Long.valueOf(expirationDuration.toSeconds()).intValue();
		// @formatter:off
		logoutCookie = Utils.<String, String>declare(jwtSecurityContext.getCookieName(), null)
				.then(Cookie::new)
				.consume(cookie -> cookie.setPath(jwtSecurityContext.getWholeDomainPath()))
				.consume(cookie -> cookie.setSecure(jwtSecurityContext.isCookieSecured()))
				.consume(cookie -> cookie.setHttpOnly(true))
				.consume(cookie -> cookie.setMaxAge(0))
				.get();
		// @formatter:on
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
				.setExpiration(Date.from(
						parseTimestamp(claims.get(jwtSecurityContext.getExpirationKey()).toString())
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

	public Cookie generateCookie(DomainUserDetails userDetails) throws Exception {
		// @formatter:off
		Cookie cookie = declare(createClaims(userDetails), userDetails.getUsername())
			.then(this::createToken)
				.prepend(jwtSecurityContext.getCookieName())
			.then(Cookie::new)
			.get();
		// @formatter:on
		cookie.setPath(jwtSecurityContext.getWholeDomainPath());
		cookie.setSecure(jwtSecurityContext.isCookieSecured());
		cookie.setHttpOnly(true);
		cookie.setMaxAge(maxAge);

		return cookie;
	}

	public Cookie getLogoutCookie() throws Exception {
		return logoutCookie;
	}

	private Map<String, Object> createClaims(DomainUserDetails userDetails) throws Exception {
		final Map<String, Object> preClaims = new HashMap<>();

		preClaims.put(jwtSecurityContext.getVersionKey(), parseTimestampString(userDetails.getVersion()));
		preClaims.put(jwtSecurityContext.getExpirationKey(),
				parseTimestampString(LocalDateTime.now().plus(expirationDuration)));

		return preClaims;
	}

	public String parseTimestampString(LocalDateTime timestamp) {
		return timestamp.toString();
	}

	public LocalDateTime parseTimestamp(String timestampString) {
		return LocalDateTime.parse(timestampString);
	}

}
