/**
 *
 */
package multicados.internal.security.jwt;

import static multicados.internal.helper.Utils.declare;
import static multicados.internal.helper.Utils.HandledFunction.identity;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import multicados.internal.helper.Common;
import multicados.internal.helper.HttpHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils.HandledFunction;
import multicados.internal.helper.Utils.TriDeclaration;
import multicados.internal.security.DomainUserDetails;
import multicados.internal.security.OnMemoryUserDetailsContext;

/**
 * @author Ngoc Huy
 *
 */
public class JWTRequestFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(JWTRequestFilter.class);

	private static final String NO_HEADERS = "JWT header not found";
	private static final String NO_COOKIES = "Unable to locate JWT cookie";

	private static final String TOKEN_IS_EXPIRED = "TOKEN EXPIRED";
	private static final String TOKEN_IS_STALE = "STALE TOKEN";

	private final UserDetailsService userDetailsService;
	private final OnMemoryUserDetailsContext onMemoryUserDetailsContext;
	private final JWTSecurityContext jwtSecurityContext;
	private final JWTStrategy jwtStrategy;

	private final ObjectMapper objectMapper;

	public JWTRequestFilter(Environment env, UserDetailsService userDetailsService,
			OnMemoryUserDetailsContext onMemoryUserDetailsContext, JWTSecurityContext jwtSecurityContext,
			ObjectMapper objectMapper) throws Exception {
		this.userDetailsService = userDetailsService;
		this.onMemoryUserDetailsContext = onMemoryUserDetailsContext;
		this.jwtSecurityContext = jwtSecurityContext;
		jwtStrategy = jwtSecurityContext.getStrategy();
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

			if (authHeader == null || !authHeader.startsWith(jwtSecurityContext.getHeaderPrefix())) {
				if (logger.isTraceEnabled()) {
					logger.trace(NO_HEADERS);
				}

				filterChain.doFilter(request, response);
				return;
			}

			Cookie cookie = WebUtils.getCookie(request, jwtSecurityContext.getCookieName());

			if (cookie == null) {
				if (logger.isTraceEnabled()) {
					logger.trace(NO_COOKIES);
				}

				filterChain.doFilter(request, response);
				return;
			}
			// @formatter:off
			declare(new Candidate(cookie, request, response))
				.flat(identity(), this::locateUserDetails)
					.third(request)
				.then(this::validate)
				.consume(this::doPostValidation);

			filterChain.doFilter(request, response);
			return;
		} catch (UsernameNotFoundException unfe) {
			if (logger.isDebugEnabled()) {
				logger.debug(Common.notFound(List.of(unfe.getMessage())));
			}
			
			filterChain.doFilter(request, response);
			return;
		} catch (Exception any) {
			any.printStackTrace();

			if (logger.isErrorEnabled()) {
				logger.error(String.format("Error while filtering request: %s", any.getMessage()));
			}

			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

			final PrintWriter writer = response.getWriter();

			try {
				if (HttpHelper.isJsonAccepted(request)) {
					writer.write(objectMapper.writeValueAsString(Common.error(Common.UNABLE_TO_COMPLETE)));
					return;
				}

				writer.write(Common.UNABLE_TO_COMPLETE);
			} finally {
				writer.flush();
			}
			
			return;
		}
	}

	private DomainUserDetails locateUserDetails(Candidate candidate) throws Exception {
		// @formatter:off
		return declare(candidate)
				.flat(identity(), this::tryLocatingFromOnMemoryContext)
				.then(this::tryLocatingFromDataSource)
				.then(DomainUserDetails.class::cast)
				.get();
		// @formatter:on
	}

	private UserDetails tryLocatingFromOnMemoryContext(Candidate candidate) {
		final UserDetails onMemoryUserDetails = onMemoryUserDetailsContext.get(candidate.getUsername());

		if (logger.isDebugEnabled()) {
			if (onMemoryUserDetails != null) {
				logger.debug("Found {} on memory cache", onMemoryUserDetails.getUsername());
			}
		}

		return onMemoryUserDetails;
	}

	private UserDetails tryLocatingFromDataSource(Candidate candidate, UserDetails onMemoryUserDetails) {
		return Optional.ofNullable(onMemoryUserDetails)
				.orElseGet(() -> userDetailsService.loadUserByUsername(candidate.getUsername()));
	}

	private TriDeclaration<Candidate, DomainUserDetails, UsernamePasswordAuthenticationToken> validate(
			Candidate candidate, DomainUserDetails userDetails) {
		final LocalDateTime now = LocalDateTime.now();

		if (candidate.getExpiration() == null || candidate.getExpiration().isBefore(now)) {
			return declare(candidate, userDetails, EXPIRED_TOKEN);
		}

		if (candidate.getVersion() == null || !candidate.getVersion().isEqual(userDetails.getVersion())) {
			return declare(candidate, userDetails, STALE_TOKEN);
		}

		final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails, null,
				userDetails.getAuthorities());

		token.setDetails(new WebAuthenticationDetailsSource().buildDetails(candidate.getRequest()));

		return declare(candidate, userDetails, token);
	}

	private void doPostValidation(
			TriDeclaration<Candidate, DomainUserDetails, UsernamePasswordAuthenticationToken> validation)
			throws Exception {
		final UsernamePasswordAuthenticationToken token = validation.getThird();
		final HttpServletRequest request = validation.getFirst().getRequest();
		final HttpServletResponse response = validation.getFirst().getResponse();

		if (token == EXPIRED_TOKEN) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			writeMessage(request, response, TOKEN_IS_EXPIRED);
			return;
		}

		if (token == STALE_TOKEN) {
			response.setStatus(HttpServletResponse.SC_OK);
			response.addCookie(jwtStrategy.getLogoutCookie());
			writeMessage(request, response, TOKEN_IS_STALE);
			return;
		}

		onMemoryUserDetailsContext.put(validation.getSecond());
		SecurityContextHolder.getContext().setAuthentication(token);
	}

	private void writeMessage(HttpServletRequest request, HttpServletResponse response, String message)
			throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(message);
		}

		if (HttpHelper.isJsonAccepted(request)) {
			HttpHelper.json(response);
			response.getWriter().write(objectMapper.writeValueAsString(Common.error(message)));
			return;
		}

		if (HttpHelper.isTextAccepted(request)) {
			HttpHelper.text(response);
			response.getWriter().write(message);
			return;
		}

		HttpHelper.all(response);
		return;
	}

	private class Candidate {

		private final Claims claims;
		private final LocalDateTime version;
		private final LocalDateTime expiration;

		private final HttpServletRequest request;
		private final HttpServletResponse response;

		public Candidate(Cookie cookie, HttpServletRequest request, HttpServletResponse response) {
			claims = jwtStrategy.extractAllClaims(cookie.getValue());
			version = locateVersion(claims);
			expiration = locateExpiration(claims);
			this.request = request;
			this.response = response;
		}

		private <T> T locateClaim(Claims claims, String claimKey, HandledFunction<String, T, Exception> claimProducer)
				throws Exception {
			String claimString = Optional.ofNullable(claims.get(claimKey)).map(Object::toString)
					.orElse(StringHelper.EMPTY_STRING);

			return claimProducer.apply(claimString);
		}

		private LocalDateTime locateVersion(Claims claims) {
			try {
				return locateClaim(claims, jwtSecurityContext.getVersionKey(),
						(versionString) -> jwtStrategy.parseTimestamp(versionString));
			} catch (Exception any) {
				return null;
			}
		}

		private LocalDateTime locateExpiration(Claims claims) {
			try {
				return locateClaim(claims, jwtSecurityContext.getExpirationKey(),
						(expirationString) -> jwtStrategy.parseTimestamp(expirationString));
			} catch (Exception any) {
				return null;
			}
		}

		public String getUsername() {
			return claims.getSubject();
		}

		public LocalDateTime getExpiration() {
			return expiration;
		}

		public LocalDateTime getVersion() {
			return version;
		}

		public HttpServletRequest getRequest() {
			return request;
		}

		public HttpServletResponse getResponse() {
			return response;
		}

	}

	private static final UsernamePasswordAuthenticationToken EXPIRED_TOKEN = new UsernamePasswordAuthenticationToken(
			null, null);
	private static final UsernamePasswordAuthenticationToken STALE_TOKEN = new UsernamePasswordAuthenticationToken(null,
			null);

}
