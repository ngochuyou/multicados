/**
 *
 */
package multicados.controller.exception;

import static multicados.internal.helper.HttpHelper.isJsonAccepted;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import multicados.internal.helper.Common;
import multicados.internal.helper.HttpHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.service.crud.security.UnauthorizedCredentialException;
import multicados.internal.service.crud.security.read.UnknownAttributesException;

/**
 * @author Ngoc Huy
 *
 */
@ControllerAdvice
public class ExceptionAdvisor extends ResponseEntityExceptionHandler {

	@ExceptionHandler(PersistenceException.class)
	public ResponseEntity<?> handlePersistenceException(PersistenceException ex, WebRequest request) {
		return PersistenceExceptionAdvisor.INSTANCE.handle(ex, request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<?> handledAccessDeniedException(AccessDeniedException ex, WebRequest request) {
		return checkForJsonOrText(ex, request, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(UnauthorizedCredentialException.class)
	public ResponseEntity<?> handledUnauthorizedCredentialException(UnauthorizedCredentialException ex,
			WebRequest request) {
		return checkForJsonOrText(ex, request, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(UnknownAttributesException.class)
	public ResponseEntity<?> handledUnknownAttributesException(UnknownAttributesException ex, WebRequest request) {
		return checkForJsonOrText(ex, request, BAD_REQUEST);
	}

	private ResponseEntity<?> checkForJsonOrText(Exception ex, WebRequest request, HttpStatus status) {
		return handleExceptionInternal(ex,
				HttpHelper.isJsonAccepted(request) ? Common.error(ex.getMessage()) : ex.getMessage(), HttpHeaders.EMPTY,
				status, request);
	}

	@Override
	protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status,
			WebRequest request) {
		final BindingResult bindingResult = ex.getBindingResult();

		if (bindingResult instanceof BeanPropertyBindingResult) {
			final BeanPropertyBindingResult beanBindingResult = ((BeanPropertyBindingResult) bindingResult);
			final String message = String.format("Following parameters are invalid: %s",
					beanBindingResult.getFieldErrors().stream().map(FieldError::getField)
							.collect(Collectors.joining(StringHelper.COMMON_JOINER)));
			final Object body = isJsonAccepted(request) ? Common.error(message) : message;

			return ResponseEntity.status(BAD_REQUEST).body(body);
		}

		return super.handleBindException(ex, headers, status, request);
	}

}
