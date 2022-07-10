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

	private final Map<Class<? extends Throwable>, BiFunction<Throwable, WebRequest, ResponseEntity<?>>> handlers;

	private PersistenceExceptionAdvisor() {
		final Map<Class<? extends Throwable>, BiFunction<Throwable, WebRequest, ResponseEntity<?>>> handlers = new HashMap<>();

		handlers.put(ConstraintViolationException.class, this::handleConstraintViolation);

		handlers.put(EntityNotFoundException.class, (enfe, request) -> resolveBody(enfe, request,
				ResponseEntity.status(HttpStatus.NOT_FOUND), enfe.getMessage()));

		handlers.put(EntityExistsException.class,
				(eee, request) -> handlers.get(EntityNotFoundException.class).apply(eee, request));

		handlers.put(HibernateException.class,
				(he, request) -> resolveBody(he, request, badRequest(), "Invalid resource"));

		handlers.put(IdentifierGenerationException.class,
				(ide, request) -> resolveBody(ide, request, badRequest(), "Unable to identify resource"));

		handlers.put(PropertyValueException.class, (pve, request) -> resolveBody(pve, request, badRequest(),
				String.format("Attribute '%s' is missing", PropertyValueException.class.cast(pve).getPropertyName())));

		this.handlers = Collections.unmodifiableMap(handlers);
	}

	ResponseEntity<?> handle(PersistenceException ex, WebRequest request) {
		final Throwable cause = ex.getCause();
		final BiFunction<Throwable, WebRequest, ResponseEntity<?>> handler = handlers
				.get(Optional.ofNullable(cause).map(Object::getClass).orElse(null));

		if (handler == null) {
			return resolveBody(cause, request, ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR),
					"Unknown error");
		}

		return handler.apply(cause, request);
	}

	private ResponseEntity<?> handleConstraintViolation(Throwable throwable, WebRequest request) {
		// TODO: turn this into handlers map or switch
		final ConstraintViolationException cve = (ConstraintViolationException) throwable;

		if (cve.getSQLException() instanceof BatchUpdateException) {
			final BatchUpdateException batchUpdateException = (BatchUpdateException) cve.getSQLException();

			if (batchUpdateException.getCause() instanceof SQLIntegrityConstraintViolationException) {
				final SQLIntegrityConstraintViolationException sqlException = (SQLIntegrityConstraintViolationException) batchUpdateException
						.getCause();
				// bellow criteria relies on database vendors
				// MySQL - InnoDB
				if (sqlException.getErrorCode() == 1062 && sqlException.getSQLState().equals("23000")) {
					return resolveBody(cve, request, ResponseEntity.status(HttpStatus.CONFLICT), Common.existed());
				}
			}
		}

		return resolveBody(cve, request, badRequest(),
				"Some of the provided information did not meet the requirements");
	}

	private ResponseEntity<?> resolveBody(Throwable cause, WebRequest request, BodyBuilder response, String message) {
		if (HttpHelper.isJsonAccepted(request)) {
			return response.body(Common.error(message));
		}

		if (HttpHelper.isTextAccepted(request)) {
			return response.body(message);
		}
		
		return response.body(null);
	}

}
