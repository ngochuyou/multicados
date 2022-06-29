/**
 *
 */
package multicados.internal.security.jwt;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * @author Ngoc Huy
 *
 */
public class JWTLogoutFilter extends AbstractAuthenticationProcessingFilter {

	private final JWTStrategy jwtStrategy;

	public JWTLogoutFilter(JWTSecurityContext jwtSecurityContext, AuthenticationManager authenticationManager) {
		super(new AntPathRequestMatcher(jwtSecurityContext.getLogoutEndpoint(), HttpMethod.POST.name()));
		this.jwtStrategy = jwtSecurityContext.getStrategy();
		setAuthenticationManager(authenticationManager);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {
		try {
			response.addCookie(jwtStrategy.getLogoutCookie());
			return null;
		} catch (Exception any) {
			throw new ServletException(any);
		}
	}



}
