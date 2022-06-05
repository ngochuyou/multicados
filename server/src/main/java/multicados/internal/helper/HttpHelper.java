/**
 * 
 */
package multicados.internal.helper;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * @author Ngoc Huy
 *
 */
public class HttpHelper {

	public static boolean isTextAccepted(HttpServletRequest request) {
		// @formatter:off
		return Optional.ofNullable(request.getHeader(HttpHeaders.ACCEPT))
				.map(value -> value.contains(MediaType.ALL_VALUE)
						|| value.contains(MediaType.TEXT_HTML_VALUE)
						|| value.contains(MediaType.TEXT_PLAIN_VALUE)
						|| value.contains(MediaType.TEXT_MARKDOWN_VALUE)
						|| value.contains(MediaType.TEXT_XML_VALUE)
						|| value.contains(MediaType.TEXT_EVENT_STREAM_VALUE))
				.orElse(false);
		// @formatter:on
	}

}
