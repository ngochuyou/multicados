/**
 * 
 */
package multicados.internal.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Ngoc Huy
 *
 */
public class FilterChainExceptionHandlingFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(FilterChainExceptionHandlingFilter.class);

	private final AccessDeniedHandler accessDeniedHandler;

	public FilterChainExceptionHandlingFilter(AccessDeniedHandler accessDeniedHandler) {
		this.accessDeniedHandler = accessDeniedHandler;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			filterChain.doFilter(request, response);
		} catch (AccessDeniedException ade) {
			if (logger.isDebugEnabled()) {
				logger.debug("Filtering {} thrown from within the filter chain",
						AccessDeniedException.class.getSimpleName());
			}

			accessDeniedHandler.handle(request, response, ade);
			return;
		} catch (Exception any) {
			throw any;
		}
	}

}
