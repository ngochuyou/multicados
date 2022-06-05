/**
 * 
 */
package multicados.internal.security.jwt;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * @author Ngoc Huy
 *
 */
class JWT {

	private final String secret;
	private final ZoneId zone;
	private final Duration expirationDuration;

	JWT(String secret, ZoneId zone, Duration expirationDuration) {
		this.secret = secret;
		this.zone = zone;
		this.expirationDuration = expirationDuration;
	}

	public Claims extractAllClaims(String token) {
		return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
	}

	public String createToken(Map<String, Object> claims, String subject) {
		// @formatter:off
		return Jwts
				.builder()
				.setClaims(claims)
				.setSubject(subject)
				.setIssuedAt(new Date(System.currentTimeMillis()))
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

}
