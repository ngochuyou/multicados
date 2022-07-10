/**
 * 
 */
package multicados.internal.security.jwt;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Indicator for detecting a JWT Authentication request
 * 
 * @author Ngoc Huy
 *
 */
public interface JwtAdvisor {

	JwtAdvice getAdvice(HttpServletRequest request);

	interface JwtAdvice {
		
		boolean isRequestingJwt();
		
		Cookie getCookie();

		String getConclusion();
		
	}
	
}
