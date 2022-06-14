/**
 * 
 */
package multicados.internal.security.jwt;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.GenericFilterBean;

import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class JWTLogoutFilter extends GenericFilterBean {

	private final RequestMatcher matcher;
	private final JWTSecurityContext jwtSecurityContext;

	public JWTLogoutFilter(JWTSecurityContext jwtSecurityContext) {
		this.jwtSecurityContext = jwtSecurityContext;
		matcher = new AntPathRequestMatcher(jwtSecurityContext.getLogoutEndpoint(), HttpMethod.POST.name());
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		if (!matcher.matches(request)) {
			chain.doFilter(request, response);
			return;
		}

		try {
			response.addCookie(getLogoutCookie(request, response));
		} catch (Exception any) {
			throw new ServletException(any);
		}
	}

	public Cookie getLogoutCookie(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// @formatter:off
		Cookie cookie = Utils.<String, String>declare(jwtSecurityContext.getCookieName(), null)
			.then(Cookie::new)
			.get();
		// @formatter:on
		cookie.setPath(jwtSecurityContext.getWholeDomainPath());
		cookie.setSecure(jwtSecurityContext.isCookieSecured());
		cookie.setHttpOnly(true);
		cookie.setMaxAge(0);

		return cookie;

	}

}
