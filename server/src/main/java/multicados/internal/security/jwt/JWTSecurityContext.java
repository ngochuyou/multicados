/**
 * 
 */
package multicados.internal.security.jwt;

import java.time.Duration;

/**
 * @author Ngoc Huy
 *
 */
public interface JWTSecurityContext {

	JWTStrategy getStrategy();

	String getHeaderPrefix();

	String getCookieName();

	String getTokenEndpoint();

	String getVersionKey();

	String getUsernameParam();

	String getPasswordParam();

	Duration getExpirationDuration();
	
	boolean isCookieSecured();
	
}
