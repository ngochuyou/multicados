/**
 *
 */
package multicados.internal.security;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.internal.helper.Common;
import multicados.internal.helper.HttpHelper;

/**
 * @author Ngoc Huy
 *
 */
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

	/**
	 *
	 */
	private static final String MESSAGE = "Access denied";
	private final ObjectMapper objectMapper;

	public AccessDeniedHandlerImpl(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

		PrintWriter writer = response.getWriter();

		if (HttpHelper.isJsonAccepted(request)) {
			HttpHelper.json(response);
			writer.write(objectMapper.writeValueAsString(Common.error(MESSAGE)));
			return;
		}

		if (HttpHelper.isTextAccepted(request)) {
			HttpHelper.text(response);
			writer.write(MESSAGE);
			return;
		}
	}

}
