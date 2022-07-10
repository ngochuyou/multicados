/**
 * 
 */
package multicados.internal.security.jwt;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.WebUtils;

/**
 * @author Ngoc Huy
 *
 */
public class HeaderBasedJwtAdvisor implements JwtAdvisor {

	private static final Logger logger = LoggerFactory.getLogger(HeaderBasedJwtAdvisor.class);

	private static final String NO_HEADER_MESSAGE = "JWT header not found";
	private static final String NO_COOKIE_MESSAGE = "Unable to locate JWT cookie";

	private final JwtSecurityContext jwtSecurityContext;

	public HeaderBasedJwtAdvisor(JwtSecurityContext jwtSecurityContext) {
		this.jwtSecurityContext = jwtSecurityContext;
	}

	@Override
	public JwtAdvice getAdvice(HttpServletRequest request) {
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (authHeader == null || !authHeader.startsWith(jwtSecurityContext.getHeaderPrefix())) {
			if (logger.isTraceEnabled()) {
				logger.trace(NO_HEADER_MESSAGE);
			}

			return NO_HEADER;
		}

		final Cookie cookie = WebUtils.getCookie(request, jwtSecurityContext.getCookieName());

		if (cookie != null) {
			return with(cookie);
		}

		if (logger.isTraceEnabled()) {
			logger.trace(NO_COOKIE_MESSAGE);
		}

		return NO_COOKIE;
	}

	private JwtAdvice with(Cookie cookie) {
		return new JwtAdvice() {

			@Override
			public boolean isRequestingJwt() {
				return true;
			}

			@Override
			public Cookie getCookie() {
				return cookie;
			}

			@Override
			public String getConclusion() {
				return "JWt Authentication detected";
			}

		};
	}

	private static final JwtAdvice NO_HEADER = new JwtAdvice() {

		@Override
		public boolean isRequestingJwt() {
			return false;
		}

		@Override
		public Cookie getCookie() {
			return null;
		}

		@Override
		public String getConclusion() {
			return NO_HEADER_MESSAGE;
		}

	};

	private static final JwtAdvice NO_COOKIE = new JwtAdvice() {
		@Override
		public boolean isRequestingJwt() {
			return false;
		}

		@Override
		public Cookie getCookie() {
			return null;
		}

		@Override
		public String getConclusion() {
			return NO_COOKIE_MESSAGE;
		}

	};

}
