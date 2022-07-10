/**
 *
 */
package multicados.internal.security.jwt;

import static multicados.internal.helper.SpringHelper.getOrDefault;
import static multicados.internal.helper.SpringHelper.getOrThrow;
import static multicados.internal.helper.Utils.declare;
import static multicados.internal.helper.Utils.HandledFunction.identity;
import static org.springframework.util.StringUtils.hasLength;

import java.time.Duration;

import org.springframework.core.env.Environment;

import multicados.internal.config.Settings;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils.HandledFunction;
import multicados.internal.locale.ZoneContext;

/**
 * @author Ngoc Huy
 *
 */
public class JwtSecurityContextImpl implements JwtSecurityContext {

	private static final Duration DEFAULT_DURATION = Duration.ofDays(7);
	private static final String DEFAULT_HEADER_PREFIX = "JWTBearer";
	private static final String DEFAULT_TOKEN_ENDPOINT = "/auth/token";
	private static final String DEFAULT_LOGOUT_ENDPOINT = "/auth/logout";
	private static final String DEFAULT_USERNAME_PARAM = "username";
	private static final String DEFAULT_PASSWORD_PARAM = "password";
	private static final String THE_WHOLE_DOMAIN = "/";

	private static final String VERSION_KEY = "version";
	private static final String EXPIRATION_KEY = "expiration";

	private final JwtStrategy strategy;
	private final String headerPrefix;
	private final String cookieName;
	private final String tokenEndpoint;
	private final String logoutEndpoint;
	private final String usernameParam;
	private final String passwordParam;
	private final boolean isCookieSecured;

	public JwtSecurityContextImpl(Environment env, ZoneContext zoneContext) throws Exception {
		HandledFunction<String, String, Exception> exact = identity();

		headerPrefix = getOrDefault(env, Settings.SECURITY_JWT_HEADER_PREFIX, exact, DEFAULT_HEADER_PREFIX);
		cookieName = getOrThrow(env, Settings.SECURITY_JWT_COOKIE_NAME, exact,
				() -> new IllegalArgumentException("Unable to locate any configured cookie name for JWT"));
		tokenEndpoint = getOrDefault(env, Settings.SECURITY_JWT_TOKEN_END_POINT, exact, DEFAULT_TOKEN_ENDPOINT);
		logoutEndpoint = getOrDefault(env, Settings.SECURITY_JWT_LOGOUT_END_POINT, exact, DEFAULT_LOGOUT_ENDPOINT);
		usernameParam = getOrDefault(env, Settings.SECURITY_JWT_TOKEN_USERNAME, exact, DEFAULT_USERNAME_PARAM);
		passwordParam = getOrDefault(env, Settings.SECURITY_JWT_TOKEN_PASSWORD, exact, DEFAULT_PASSWORD_PARAM);
		isCookieSecured = env.getProperty(Settings.ACTIVE_PROFILES).equals(Settings.DEFAULT_PRODUCTION_PROFILE);
		strategy = buildJWTContext(env, zoneContext);
	}

	private JwtStrategy buildJWTContext(Environment env, ZoneContext zoneContext) throws Exception {
		// @formatter:off
		return declare(env)
			.flat(this::locateSecret, self -> zoneContext.getZone(), this::locateDuration)
			.then((secret, zone, duration) -> new JwtStrategy(this, secret, zone, duration))
			.get();
		// @formatter:on
	}

	private Duration locateDuration(Environment env) {
		String configuredDuration = env.getProperty(Settings.SECURITY_JWT_EXPIRATION_DURATION);

		if (!hasLength(configuredDuration)) {
			return DEFAULT_DURATION;
		}

		if (!StringHelper.isNumeric(configuredDuration)) {
			throw new IllegalArgumentException(String.format("%s is not a number", configuredDuration));
		}

		return Duration.ofMillis(Long.parseLong(configuredDuration));
	}

	private String locateSecret(Environment env) throws Exception {
		return getOrThrow(env, Settings.SECURITY_JWT_SECRET, identity(),
				() -> new IllegalArgumentException("Unable to locate any configured secret key for JWT"));
	}

	@Override
	public JwtStrategy getStrategy() {
		return strategy;
	}

	@Override
	public String getCookieName() {
		return cookieName;
	}

	@Override
	public String getHeaderPrefix() {
		return headerPrefix;
	}

	@Override
	public String getTokenEndpoint() {
		return tokenEndpoint;
	}

	@Override
	public String getVersionKey() {
		return VERSION_KEY;
	}

	@Override
	public String getExpirationKey() {
		return EXPIRATION_KEY;
	}

	@Override
	public String getUsernameParam() {
		return usernameParam;
	}

	@Override
	public String getPasswordParam() {
		return passwordParam;
	}

	@Override
	public boolean isCookieSecured() {
		return isCookieSecured;
	}

	@Override
	public String getLogoutEndpoint() {
		return logoutEndpoint;
	}

	@Override
	public String getWholeDomainPath() {
		return THE_WHOLE_DOMAIN;
	}

}
