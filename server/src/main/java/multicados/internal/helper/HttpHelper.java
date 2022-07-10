/**
 *
 */
package multicados.internal.helper;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Ngoc Huy
 *
 */
public interface HttpHelper {
	
	public static boolean tryJson(HttpServletRequest request, HttpServletResponse response, ObjectMapper mapper,
			Object body, boolean doCloseWriter) throws IOException {
		if (!isJsonAccepted(request)) {
			return false;
		}

		final PrintWriter writer = response.getWriter();

		try {
			writer.write(mapper.writeValueAsString(body));
			json(response);
			return true;
		} finally {
			writer.flush();

			if (doCloseWriter) {
				writer.close();
			}
		}
	}

	public static boolean tryText(HttpServletRequest request, HttpServletResponse response, String body,
			boolean doCloseWriter) throws IOException {
		if (!isJsonAccepted(request)) {
			return false;
		}

		final PrintWriter writer = response.getWriter();

		try {
			writer.write(body);
			text(response);
			return true;
		} finally {
			writer.flush();

			if (doCloseWriter) {
				writer.close();
			}
		}
	}

	public static boolean isTextAccepted(HttpServletRequest request) {
		return hasText(request.getHeader(HttpHeaders.ACCEPT));
	}

	public static boolean isTextAccepted(WebRequest request) {
		return hasText(request.getHeader(HttpHeaders.ACCEPT));
	}

	private static boolean hasText(String headerValue) {
		// @formatter:off
		return Optional.ofNullable(headerValue)
				.map(value -> value.contains(MediaType.ALL_VALUE)
						|| value.contains(MediaType.TEXT_PLAIN_VALUE)
						|| value.contains(MediaType.TEXT_HTML_VALUE)
						|| value.contains(MediaType.TEXT_EVENT_STREAM_VALUE)
						|| value.contains(MediaType.TEXT_MARKDOWN_VALUE)
						|| value.contains(MediaType.TEXT_XML_VALUE))
				.orElse(false);
		// @formatter:on
	}

	private static boolean hasJson(String headerValue) {
		// @formatter:off
		return Optional.ofNullable(headerValue)
				.map(value -> value.contains(MediaType.ALL_VALUE)
						|| value.contains(MediaType.APPLICATION_JSON_VALUE)
						|| value.contains(MediaType.APPLICATION_NDJSON_VALUE)
						|| value.contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
				.orElse(false);
		// @formatter:on
	}

	public static boolean isJsonAccepted(HttpServletRequest request) {
		return hasJson(request.getHeader(HttpHeaders.ACCEPT));
	}

	public static boolean isJsonAccepted(WebRequest request) {
		return hasJson(request.getHeader(HttpHeaders.ACCEPT));
	}

	public static HttpServletResponse all(HttpServletResponse response) {
		return content(response, List.of(MediaType.ALL_VALUE));
	}

	public static HttpServletResponse json(HttpServletResponse response) {
		return content(response, List.of(MediaType.APPLICATION_JSON_VALUE));
	}

	public static HttpServletResponse text(HttpServletResponse response) {
		return content(response, List.of(MediaType.TEXT_HTML_VALUE));
	}

	public static HttpServletResponse image(HttpServletResponse response) {
		return content(response,
				List.of(MediaType.IMAGE_GIF_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE));
	}

	public static HttpServletResponse content(HttpServletResponse response, Collection<String> mediaTypes) {
		response.setHeader(HttpHeaders.CONTENT_TYPE, StringHelper.join(StringHelper.COMMA, mediaTypes));
		return response;
	}

	public static void attachCookie(HttpServletResponse response, Cookie cookie) {
		response.addCookie(cookie);
	}

	public static Cookie createHttpOnlyCookie(String cookieName, String value) {
		final Cookie cookie = new Cookie(cookieName, value);

		cookie.setHttpOnly(true);
		cookie.setSecure(false);

		return cookie;
	}

	public static Cookie createInvalidateHttpOnlyCookie(String cookieName) {
		final Cookie cookie = createHttpOnlyCookie(cookieName, StringHelper.EMPTY_STRING);

		cookie.setMaxAge(0);

		return cookie;
	}

}
