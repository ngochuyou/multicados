/**
 *
 */
package multicados.internal.security.jwt;

/**
 * @author Ngoc Huy
 *
 */
public interface JwtSecurityContext {

	JwtStrategy getStrategy();

	String getHeaderPrefix();

	String getCookieName();

	String getTokenEndpoint();

	String getLogoutEndpoint();

	String getVersionKey();

	String getExpirationKey();

	String getUsernameParam();

	String getPasswordParam();

	String getWholeDomainPath();

	boolean isCookieSecured();

}
