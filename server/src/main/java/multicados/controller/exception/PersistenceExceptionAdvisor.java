/**
 * 
 */
package multicados.controller.exception;

import static org.springframework.http.ResponseEntity.badRequest;

import java.sql.BatchUpdateException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.id.IdentifierGenerationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.context.request.WebRequest;

import multicados.internal.helper.Common;
import multicados.internal.helper.HttpHelper;

/**
 * @author Ngoc Huy
 *
 */
class PersistenceExceptionAdvisor {

	static final PersistenceExceptionAdvisor INSTANCE = new PersistenceExceptionAdvisor();

	private final Map<Class<? extends Throwable>, BiFunction<Throwable, WebRequest, ResponseEntity<Object>>> handlers;

	private PersistenceExceptionAdvisor() {
		final Map<Class<? extends Throwable>, BiFunction<Throwable, WebRequest, ResponseEntity<Object>>> handlerCandidates = new HashMap<>();

		handlerCandidates.put(ConstraintViolationException.class, this::handleConstraintViolation);

		handlerCandidates.put(EntityNotFoundException.class, (enfe, request) -> resolveBody(request,
				ResponseEntity.status(HttpStatus.NOT_FOUND), enfe.getMessage()));

		handlerCandidates.put(EntityExistsException.class,
				(eee, request) -> handlerCandidates.get(EntityNotFoundException.class).apply(eee, request));

		handlerCandidates.put(HibernateException.class,
				(he, request) -> resolveBody(request, badRequest(), "Invalid resource"));

		handlerCandidates.put(IdentifierGenerationException.class,
				(ide, request) -> resolveBody(request, badRequest(), "Unable to identify resource"));

		handlerCandidates.put(PropertyValueException.class, (pve, request) -> resolveBody(request, badRequest(),
				String.format("Attribute '%s' is missing", PropertyValueException.class.cast(pve).getPropertyName())));

		this.handlers = Collections.unmodifiableMap(handlerCandidates);
	}

	ResponseEntity<Object> handle(PersistenceException ex, WebRequest request) {
		final Throwable cause = ex.getCause();
		final BiFunction<Throwable, WebRequest, ResponseEntity<Object>> handler = handlers
				.get(Optional.ofNullable(cause).map(Object::getClass).orElse(null));

		if (handler == null) {
			return resolveBody(request, ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR), "Unknown error");
		}

		return handler.apply(cause, request);
	}

	private ResponseEntity<Object> handleConstraintViolation(Throwable throwable, WebRequest request) {
		// TODO: turn this into handlers map or switch
		final ConstraintViolationException cve = (ConstraintViolationException) throwable;
		// bellow criteria relies on database vendors
		// MySQL - InnoDB
		if (cve.getSQLException() instanceof BatchUpdateException batchUpdateException
				&& batchUpdateException.getCause() instanceof SQLIntegrityConstraintViolationException sqlException
				&& sqlException.getErrorCode() == 1062 && sqlException.getSQLState().equals("23000")) {
			return resolveBody(request, ResponseEntity.status(HttpStatus.CONFLICT), Common.existed());
		}

		return resolveBody(request, badRequest(), "Some of the provided information did not meet the requirements");
	}

	private ResponseEntity<Object> resolveBody(WebRequest request, BodyBuilder response, String message) {
		if (HttpHelper.isJsonAccepted(request)) {
			return response.body(Common.error(message));
		}

		if (HttpHelper.isTextAccepted(request)) {
			return response.body(message);
		}

		return response.body(null);
	}

}
