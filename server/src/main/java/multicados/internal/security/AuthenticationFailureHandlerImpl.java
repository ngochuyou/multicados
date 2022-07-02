/**
 *
 */
package multicados.internal.security;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.internal.helper.Common;
import multicados.internal.helper.HttpHelper;

/**
 * @author Ngoc Huy
 *
 */
public class AuthenticationFailureHandlerImpl implements AuthenticationFailureHandler {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFailureHandlerImpl.class);

	private final ObjectMapper mapper;

	public AuthenticationFailureHandlerImpl(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {

		if (exception != null) {
			if (exception instanceof UsernameNotFoundException) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				writeBody(request, response, exception);
				return;
			}

			if (exception instanceof LockedException) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				writeBody(request, response, exception);
				return;
			}

			if (logger.isErrorEnabled()) {
				logger.error(exception.getMessage());
			}

			writeBody(request, response, exception);
		}

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	}

	private void writeBody(HttpServletRequest request, HttpServletResponse response, Throwable exception)
			throws IOException {
		final PrintWriter writer = response.getWriter();

		try {
			if (HttpHelper.isJsonAccepted(request)) {
				HttpHelper.json(response);
				writer.write(mapper.writeValueAsString(Common.error(exception.getMessage())));
				return;
			}

			writer.write(exception.getMessage());

			if (HttpHelper.isTextAccepted(request)) {
				HttpHelper.text(response);
				return;
			}

			HttpHelper.all(response);
			return;
		} finally {
			writer.flush();
			writer.close();
		}
	}

}
