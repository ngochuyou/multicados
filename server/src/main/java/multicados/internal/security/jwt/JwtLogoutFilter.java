/**
 *
 */
package multicados.internal.security.jwt;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.internal.helper.Common;
import multicados.internal.helper.HttpHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.security.jwt.JwtAdvisor.JwtAdvice;

/**
 * @author Ngoc Huy
 *
 */
public class JwtLogoutFilter extends AbstractAuthenticationProcessingFilter {

	private static final String SUCCESSFULLY_LOGGED_OUT = "SUCCESSFULLY LOGGED OUT";

	private final JwtStrategy jwtStrategy;
	private final JwtAdvisor jwtAdvisor;

	private final ObjectMapper mapper;

	private static final String JWT_ADVICE = "jwt_advice";
	@SuppressWarnings("serial")
	private static final AuthenticationException ADVISOR_DENIAL = new AuthenticationException(
			StringHelper.EMPTY_STRING) {};

	public JwtLogoutFilter(JwtSecurityContext jwtSecurityContext, AuthenticationManager authenticationManager,
			ObjectMapper mapper, JwtAdvisor jwtAdvisor) {
		super(new AntPathRequestMatcher(jwtSecurityContext.getLogoutEndpoint(), HttpMethod.POST.name()));
		this.jwtStrategy = jwtSecurityContext.getStrategy();
		this.jwtAdvisor = jwtAdvisor;
		this.mapper = mapper;
		setAuthenticationManager(authenticationManager);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {
		final JwtAdvice advice = jwtAdvisor.getAdvice(request);

		if (!advice.isRequestingJwt()) {
			// attach the advice
			request.setAttribute(JWT_ADVICE, advice);
			throw ADVISOR_DENIAL;
		}
		// TODO: validate token
		response.addCookie(jwtStrategy.getLogoutCookie());
		// return null so the filter returns immediately
		if (HttpHelper.tryJson(request, response, mapper, Common.payload(SUCCESSFULLY_LOGGED_OUT), true)) {
			return null;
		}

		if (HttpHelper.tryText(request, response, SUCCESSFULLY_LOGGED_OUT, true)) {
			return null;
		}

		return null;
	}

	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {
		// advice.isRequestingJwt() SURELY gives false here, otherwise there's a fraud
		if (failed == ADVISOR_DENIAL) {
			final JwtAdvice advice = (JwtAdvice) request.getAttribute(JWT_ADVICE);
			// jwt authentication was requested but failed to complete
			tryToMakeDenial(request, response, advice.getConclusion());
			request.removeAttribute(JWT_ADVICE);
			return;
		}
		// most likely to never happen
		super.unsuccessfulAuthentication(request, response, failed);
	}

	private boolean tryToMakeDenial(HttpServletRequest request, HttpServletResponse response, String conclusion)
			throws IOException {
		if (HttpHelper.tryJson(request, response, mapper, Common.error(conclusion), true)) {
			return true;
		}

		if (HttpHelper.tryText(request, response, conclusion, true)) {
			return true;
		}

		return false;
	}

}
