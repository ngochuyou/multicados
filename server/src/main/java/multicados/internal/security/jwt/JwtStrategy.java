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

import javax.servlet.http.Cookie;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import multicados.internal.helper.HttpHelper;
import multicados.internal.helper.Utils;
import multicados.internal.security.DomainUserDetails;

/**
 * @author Ngoc Huy
 *
 */
class JwtStrategy {

	private final JwtSecurityContext jwtSecurityContext;

	private final String secret;
	private final ZoneId zone;
	private final Duration expirationDuration;
	private final int maxAge;

	private final Cookie logoutCookie;

	JwtStrategy(JwtSecurityContext jwtSecurityContext, String secret, ZoneId zone, Duration expirationDuration)
			throws Exception {
		this.jwtSecurityContext = jwtSecurityContext;
		this.secret = secret;
		this.zone = zone;
		this.expirationDuration = expirationDuration;
		maxAge = Long.valueOf(expirationDuration.toSeconds()).intValue();
		// @formatter:off
		logoutCookie = Utils.declare(jwtSecurityContext.getCookieName())
				.then(HttpHelper::createInvalidateHttpOnlyCookie)
				.consume(cookie -> cookie.setPath(jwtSecurityContext.getWholeDomainPath()))
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

	public Cookie generateCookie(DomainUserDetails userDetails) throws Exception {
		// @formatter:off
		final Cookie cookie = declare(createClaims(userDetails), userDetails.getUsername())
			.then(this::createToken)
				.prepend(jwtSecurityContext.getCookieName())
			.then(HttpHelper::createHttpOnlyCookie)
			.get();
		// @formatter:on
		cookie.setPath(jwtSecurityContext.getWholeDomainPath());
		cookie.setMaxAge(maxAge);

		return cookie;
	}

	public Cookie getLogoutCookie() {
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
