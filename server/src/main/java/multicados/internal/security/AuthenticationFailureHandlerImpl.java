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

import com.fasterxml.jackson.core.JsonProcessingException;
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
			if (logger.isErrorEnabled()) {
				logger.error(exception.getMessage());
			}

			response.getWriter().write(resolveBody(request, response, exception));
			response.getWriter().flush();
		}

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	}

	private String resolveBody(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws JsonProcessingException {
		if (HttpHelper.isJsonAccepted(request)) {
			HttpHelper.json(response);
			return mapper.writeValueAsString(Common.error(exception.getMessage()));
		}

		if (HttpHelper.isTextAccepted(request)) {
			HttpHelper.text(response);
			return exception.getMessage();
		}

		HttpHelper.all(response);
		return null;
	}

}
