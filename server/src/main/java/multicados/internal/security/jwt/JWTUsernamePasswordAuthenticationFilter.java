/**
 * 
 */
package multicados.internal.security.jwt;

import static multicados.internal.helper.Utils.declare;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.internal.helper.Common;
import multicados.internal.helper.HttpHelper;
import multicados.internal.security.DomainUserDetails;
import multicados.internal.security.OnMemoryUserDetailsContext;

/**
 * @author Ngoc Huy
 *
 */
public class JWTUsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	private final OnMemoryUserDetailsContext onMemoryUserDetailsContext;
	private final JWTSecurityContext jwtSecurityContext;
	private final JWTStrategy jwtStrategy;
	private final ObjectMapper objectMapper;

	private final BadCredentialsException usernameNotFoundException;
	private final BadCredentialsException passwordNotFoundException;

	private final int maxAge;
	private final boolean isCookieSecured;
	private static final String SUCCESSFULLY_LOGGED_IN = "SUCCESSFULLY LOGGED IN";

	public JWTUsernamePasswordAuthenticationFilter(OnMemoryUserDetailsContext onMemoryUserDetailsContext,
			JWTSecurityContext jwtSecurityContext, AuthenticationFailureHandler authenticationFailureHandler,
			AuthenticationManager authenticationManager, ObjectMapper objectMapper) {
		super(new AntPathRequestMatcher(jwtSecurityContext.getTokenEndpoint(), HttpMethod.POST.name()),
				authenticationManager);
		this.onMemoryUserDetailsContext = onMemoryUserDetailsContext;
		this.jwtSecurityContext = jwtSecurityContext;
		jwtStrategy = jwtSecurityContext.getStrategy();
		maxAge = Long.valueOf(jwtStrategy.getExpirationDuration().toSeconds()).intValue();
		isCookieSecured = this.jwtSecurityContext.isCookieSecured();
		setAuthenticationFailureHandler(authenticationFailureHandler);
		setAuthenticationManager(authenticationManager);
		this.objectMapper = objectMapper;

		usernameNotFoundException = new BadCredentialsException(Common.notEmpty(jwtSecurityContext.getUsernameParam()));
		passwordNotFoundException = new BadCredentialsException(Common.notEmpty(jwtSecurityContext.getPasswordParam()));
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {
		// TODO Auto-generated method stub
		String username = Optional.ofNullable(request.getParameter(jwtSecurityContext.getUsernameParam()))
				.orElseThrow(() -> usernameNotFoundException);
		String password = Optional.ofNullable(request.getParameter(jwtSecurityContext.getPasswordParam()))
				.orElseThrow(() -> passwordNotFoundException);
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);

		return this.getAuthenticationManager().authenticate(token);
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Authentication authentication) throws IOException, ServletException {
		DomainUserDetails userDetails = (DomainUserDetails) authentication.getPrincipal();

		onMemoryUserDetailsContext.put(userDetails);

		try {
			response.addCookie(generateCookie(userDetails));
			response.setStatus(HttpStatus.OK.value());

			if (HttpHelper.isTextAccepted(request)) {
				response.getWriter().write(SUCCESSFULLY_LOGGED_IN);
				response.getWriter().flush();
				HttpHelper.text(response);
				return;
			}

			if (HttpHelper.isJsonAccepted(request)) {
				response.getWriter().write(objectMapper.writeValueAsString(Common.payload(SUCCESSFULLY_LOGGED_IN)));
				response.getWriter().flush();
				HttpHelper.json(response);
				return;
			}
		} catch (Exception any) {
			throw new ServletException(any);
		}
	}

	private Cookie generateCookie(DomainUserDetails userDetails) throws Exception {
		// @formatter:off
		Cookie cookie = declare(createClaims(userDetails), userDetails.getUsername())
			.then(jwtStrategy::createToken)
				.prepend(jwtSecurityContext.getCookieName())
			.then(Cookie::new)
			.get();
		// @formatter:on
		cookie.setPath(jwtSecurityContext.getWholeDomainPath());
		cookie.setSecure(isCookieSecured);
		cookie.setHttpOnly(true);
		cookie.setMaxAge(maxAge);

		return cookie;
	}

	private Map<String, Object> createClaims(DomainUserDetails userDetails) throws Exception {
		final Map<String, Object> preClaims = new HashMap<>();

		preClaims.put(jwtSecurityContext.getVersionKey(),
				userDetails.getVersion().atZone(jwtStrategy.getZone()).toInstant().toEpochMilli());

		return preClaims;
	}

}
