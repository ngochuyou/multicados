/**
 * 
 */
package multicados.internal.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * @author Ngoc Huy
 *
 */
public class AccessDeniedAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final AccessDeniedHandler accessDeniedHandler;

	public AccessDeniedAuthenticationEntryPoint(AccessDeniedHandler accessDeniedHandler) {
		this.accessDeniedHandler = accessDeniedHandler;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		accessDeniedHandler.handle(request, response, null);
	}

}
