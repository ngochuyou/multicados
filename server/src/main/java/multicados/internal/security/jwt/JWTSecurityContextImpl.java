/**
 * 
 */
package multicados.internal.security.jwt;

import static java.util.function.Function.identity;
import static multicados.internal.helper.SpringHelper.getOrDefault;
import static multicados.internal.helper.SpringHelper.getOrThrow;
import static multicados.internal.helper.Utils.declare;
import static org.springframework.util.StringUtils.hasLength;

import java.time.Duration;
import java.time.ZoneId;
import java.util.function.Function;

import org.springframework.core.env.Environment;

import multicados.internal.config.Settings;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
public class JWTSecurityContextImpl implements JWTSecurityContext {

	private static final String LOCAL_ZONE = "LOCAL";
	private static final Duration DEFAULT_DURATION = Duration.ofDays(7);
	private static final String DEFAULT_HEADER_PREFIX = "JWTBearer";
	private static final String DEFAULT_TOKEN_ENDPOINT = "/auth/token";
	private static final String DEFAULT_USERNAME_PARAM = "username";
	private static final String DEFAULT_PASSWORD_PARAM = "password";

	private static final String VERSION_KEY = "version";

	private final JWTStrategy strategy;
	private final String headerPrefix;
	private final String cookieName;
	private final String tokenEndpoint;
	private final String usernameParam;
	private final String passwordParam;
	private final boolean isCookieSecured;

	public JWTSecurityContextImpl(Environment env) throws Exception {
		Function<String, String> exact = identity();

		headerPrefix = getOrDefault(env, Settings.SECURITY_JWT_HEADER_PREFIX, exact, DEFAULT_HEADER_PREFIX);
		cookieName = getOrThrow(env, Settings.SECURITY_JWT_COOKIE_NAME, exact,
				() -> new IllegalArgumentException("Unable to locate any configured cookie name for JWT"));
		tokenEndpoint = getOrDefault(env, Settings.SECURITY_JWT_TOKEN_END_POINT, exact, DEFAULT_TOKEN_ENDPOINT);
		usernameParam = getOrDefault(env, Settings.SECURITY_JWT_TOKEN_USERNAME, exact, DEFAULT_USERNAME_PARAM);
		passwordParam = getOrDefault(env, Settings.SECURITY_JWT_TOKEN_PASSWORD, exact, DEFAULT_PASSWORD_PARAM);
		isCookieSecured = env.getProperty(Settings.ACTIVE_PROFILES).equals(Settings.PRODUCTION_PROFILE);
		strategy = buildJWTContext(env);
	}

	private JWTStrategy buildJWTContext(Environment env) throws Exception {
		// @formatter:off
		return declare(env)
			.flat(this::locateSecret, this::locateZone, this::locateDuration)
			.then(JWTStrategy::new)
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

	private ZoneId locateZone(Environment env) {
		String configuredZone = env.getProperty(Settings.SECURITY_JWT_ZONE);

		if (!hasLength(configuredZone) || LOCAL_ZONE.equals(configuredZone.toUpperCase())) {
			return ZoneId.systemDefault();
		}

		if (!ZoneId.SHORT_IDS.containsKey(configuredZone)) {
			throw new IllegalArgumentException(String.format("Unknown zone id [%s]", configuredZone));
		}

		return ZoneId.of(ZoneId.SHORT_IDS.get(configuredZone));
	}

	private String locateSecret(Environment env) throws Exception {
		return getOrThrow(env, Settings.SECURITY_JWT_SECRET, identity(),
				() -> new IllegalArgumentException("Unable to locate any configured secret key for JWT"));
	}

	@Override
	public JWTStrategy getStrategy() {
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
	public String getUsernameParam() {
		return usernameParam;
	}

	@Override
	public String getPasswordParam() {
		return passwordParam;
	}

	@Override
	public Duration getExpirationDuration() {
		return strategy.getExpirationDuration();
	}

	@Override
	public boolean isCookieSecured() {
		return isCookieSecured;
	}

}
