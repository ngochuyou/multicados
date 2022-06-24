/**
 * 
 */
package multicados.controller.controllers;

import static multicados.internal.helper.Common.error;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import multicados.domain.entity.Role;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.Common;
import multicados.internal.helper.HttpHelper;
import multicados.internal.service.ServiceResult;
import multicados.security.userdetails.UserDetailsServiceImpl.DomainUser;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractController {

	protected static final DomainUser ANONYMOUS = new DomainUser("anonymous", "anonymous", false, false,
			LocalDateTime.now(), List.of(new SimpleGrantedAuthority(Role.ANONYMOUS.name())));

	/**
	 * Construct a {@link ResponseEntity} body based on the
	 * {@link HttpHeaders.CONTENT_TYPE} with 404 code
	 * 
	 * @param request
	 * @param preficies the message which is included in the body
	 * @return the {@link ResponseEntity}
	 */
	protected ResponseEntity<?> sendNotFound(HttpServletRequest request, Collection<String> preficies) {
		if (HttpHelper.isJsonAccepted(request)) {
			return sendNotFound(error(Common.notFound(preficies)));
		}

		if (HttpHelper.isTextAccepted(request)) {
			return sendNotFound(Common.notFound(preficies));
		}

		return sendNotFound(null);
	}

	protected <T> ResponseEntity<?> sendNotFound(T body) {
		return status(HttpStatus.NOT_FOUND).body(body);
	}

	/**
	 * Construct a {@link ResponseEntity} body based on the
	 * {@link HttpHeaders.CONTENT_TYPE} with 200 code
	 * 
	 * @param <T>     body type
	 * @param request
	 * @param body    the data to be included in the response body
	 * @return
	 */
	protected <T> ResponseEntity<?> sendOk(HttpServletRequest request, T body) {
		if (HttpHelper.isJsonAccepted(request)) {
			return ok(Common.payload(body));
		}

		if (HttpHelper.isTextAccepted(request)) {
			return ok(body.toString());
		}

		return ok(body);
	}

	protected <T> ResponseEntity<?> sendResult(ServiceResult result, T body) throws Exception {
		if (result.isOk()) {
			return ok(body);
		}

		if (result.getException() != null) {
			throw result.getException();
		}
		// @formatter:off
		return status(HttpStatus.BAD_REQUEST)
				.body(result.getValidation().getErrors().entrySet().stream()
					.map(entry -> Map.entry(entry.getKey(), entry.getValue().getMessage()))
					.collect(CollectionHelper.toMap()));
		// @formatter:on
	}

}
