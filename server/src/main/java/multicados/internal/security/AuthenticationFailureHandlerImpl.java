/**
 * 
 */
package multicados.internal.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * @author Ngoc Huy
 *
 */
public class AuthenticationFailureHandlerImpl implements AuthenticationFailureHandler {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFailureHandlerImpl.class);

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		if (exception != null && logger.isErrorEnabled()) {
			logger.error(exception.getMessage());
			response.getWriter().write(exception.getMessage());
			response.getWriter().flush();
		}

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	}

}
