/**
 *
 */
package multicados.controller.controllers;

import static multicados.internal.helper.HttpHelper.isJsonAccepted;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
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

	private final Map<Class<? extends Throwable>, BiFunction<Throwable, WebRequest, ResponseEntity<?>>> persistenceExceptionHandler;

	public ExceptionAdvisor() {
		final Map<Class<? extends Throwable>, BiFunction<Throwable, WebRequest, ResponseEntity<?>>> persistenceExceptionHandler = new HashMap<>();

		persistenceExceptionHandler.put(ConstraintViolationException.class, (cause, request) -> {
			BodyBuilder response = ResponseEntity.status(HttpStatus.BAD_REQUEST);

			if (HttpHelper.isJsonAccepted(request)) {
				return response.body(Common.error("Some of the provided information did not meet the guideline"));
			}

			return response.body(cause.getMessage());
		});

		this.persistenceExceptionHandler = Collections.unmodifiableMap(persistenceExceptionHandler);
	}

//	private BodyBuilder text(BodyBuilder response) {
//		return response.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.TEXT_HTML_VALUE,
//				MediaType.TEXT_MARKDOWN_VALUE, MediaType.TEXT_PLAIN_VALUE, MediaType.TEXT_XML_VALUE);
//	}
//
//	private BodyBuilder json(BodyBuilder response) {
//		return response.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
//	}

	@ExceptionHandler(PersistenceException.class)
	public ResponseEntity<?> handlePersistenceException(PersistenceException ex, WebRequest request) {
		final Throwable cause = ex.getCause();
		final BiFunction<Throwable, WebRequest, ResponseEntity<?>> handler = persistenceExceptionHandler
				.get(cause.getClass());

		if (handler == null) {
			final String message = "Unknown error";

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(!isJsonAccepted(request) ? message : Common.error(message));
		}

		return handler.apply(cause, request);
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
