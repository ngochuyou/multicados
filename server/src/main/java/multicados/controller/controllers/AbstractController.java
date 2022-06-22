/**
 * 
 */
package multicados.controller.controllers;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import multicados.domain.entity.Role;
import multicados.internal.helper.Common;
import multicados.internal.helper.HttpHelper;
import multicados.security.userdetails.UserDetailsServiceImpl.DomainUser;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractController {

	protected static final DomainUser ANONYMOUS = new DomainUser("anonymous", "anonymous", false, false,
			LocalDateTime.now(), List.of(new SimpleGrantedAuthority(Role.ANONYMOUS.name())));

	protected ResponseEntity<?> notFound(HttpServletRequest request, Collection<String> preficies) {
		if (HttpHelper.isJsonAccepted(request)) {
			return notFound(Common.error(Common.notFound(preficies)));
		}

		if (HttpHelper.isTextAccepted(request)) {
			return notFound(Common.notFound(preficies));
		}

		return notFound(null);
	}

	protected <T> ResponseEntity<?> notFound(T body) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	protected <T> ResponseEntity<?> ok(HttpServletRequest request, T body) {
		if (HttpHelper.isJsonAccepted(request)) {
			return ResponseEntity.ok(Common.payload(body));
		}

		if (HttpHelper.isTextAccepted(request)) {
			return ResponseEntity.ok(body.toString());
		}

		return ResponseEntity.ok(body);
	}

}
