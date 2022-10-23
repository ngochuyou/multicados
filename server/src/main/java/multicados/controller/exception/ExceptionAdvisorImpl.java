/**
 *
 */
package multicados.controller.exception;

import static multicados.internal.helper.HttpHelper.isJsonAccepted;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

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

import multicados.internal.context.Loggable;
import multicados.internal.helper.Common;
import multicados.internal.helper.HttpHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.BiDeclaration;
import multicados.internal.service.crud.security.UnauthorizedCredentialException;
import multicados.internal.service.crud.security.read.UnknownAttributesException;

/**
 * @author Ngoc Huy
 *
 */
@ControllerAdvice
public class ExceptionAdvisorImpl extends ResponseEntityExceptionHandler implements ExceptionAdvisor, Loggable {

	private final Map<HandlerKey, BiFunction<Throwable, WebRequest, BiDeclaration<HttpStatus, Object>>> handlersMap = new HashMap<>();

	private final AtomicBoolean isClosed = new AtomicBoolean(false);

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatus status, WebRequest request) {
		final BodyBuilder builder = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
		final String message = Common.UNABLE_TO_COMPLETE;

		if (HttpHelper.isJsonAccepted(request)) {
			return builder.body(Common.error(message));
		}

		if (HttpHelper.isTextAccepted(request)) {
			return builder.body(message);
		}

		return builder.body(null);
	}

	@ExceptionHandler(PersistenceException.class)
	public ResponseEntity<Object> handlePersistenceException(PersistenceException ex, WebRequest request) {
		return PersistenceExceptionAdvisor.INSTANCE.handle(ex, request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Object> handledAccessDeniedException(AccessDeniedException ex, WebRequest request) {
		return checkForJsonOrText(ex, request, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(UnauthorizedCredentialException.class)
	public ResponseEntity<Object> handledUnauthorizedCredentialException(UnauthorizedCredentialException ex,
			WebRequest request) {
		return checkForJsonOrText(ex, request, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(UnknownAttributesException.class)
	public ResponseEntity<Object> handledUnknownAttributesException(UnknownAttributesException ex, WebRequest request) {
		return checkForJsonOrText(ex, request, BAD_REQUEST);
	}

	private ResponseEntity<Object> checkForJsonOrText(Exception ex, WebRequest request, HttpStatus status) {
		return handleExceptionInternal(ex,
				HttpHelper.isJsonAccepted(request) ? Common.error(ex.getMessage()) : ex.getMessage(), HttpHeaders.EMPTY,
				status, request);
	}

	@Override
	protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status,
			WebRequest request) {
		final BindingResult bindingResult = ex.getBindingResult();

		if (bindingResult instanceof BeanPropertyBindingResult beanBindingResult) {
			final String message = String.format("Following parameters are invalid: %s",
					beanBindingResult.getFieldErrors().stream().map(FieldError::getField)
							.collect(Collectors.joining(StringHelper.COMMON_JOINER)));
			final Object body = isJsonAccepted(request) ? Common.error(message) : message;

			return ResponseEntity.status(BAD_REQUEST).body(body);
		}

		return super.handleBindException(ex, headers, status, request);
	}

	@ExceptionHandler(AdvisedException.class)
	public ResponseEntity<Object> handledScopedException(AdvisedException se, WebRequest request) {
		final HandlerKey handlerKey = new HandlerKey(se);
		final BiDeclaration<HttpStatus, Object> advice = handlersMap.get(handlerKey).apply(se.getCause(), request);

		return ResponseEntity.status(advice.getFirst()).body(Common.error(advice.getSecond()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Throwable> void register(Class<?> sourceType, Class<T> exceptionType,
			BiFunction<T, WebRequest, BiDeclaration<HttpStatus, Object>> handler) throws IllegalAccessException {
		if (isClosed.get()) {
			throw new IllegalAccessException(Utils.Access.getClosedMessage(this));
		}

		final HandlerKey key = new HandlerKey(sourceType, exceptionType);

		if (handlersMap.containsKey(key)) {
			throw new IllegalArgumentException(String.format("Key %s has alreay existed", key));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Registering a new handler %s with key %s", handler, key));
		}

		handlersMap.put(key, (BiFunction<Throwable, WebRequest, BiDeclaration<HttpStatus, Object>>) handler);
	}

	private static class HandlerKey {

		private final Class<?> sourceType;
		private final Class<?> exceptionType;

		public HandlerKey(Class<?> sourceType, Class<?> exceptionType) {
			this.sourceType = sourceType;
			this.exceptionType = exceptionType;
		}

		public HandlerKey(AdvisedException se) {
			this.sourceType = se.getSourceType();
			this.exceptionType = se.getCause().getClass();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;

			result = prime * result + ((exceptionType == null) ? 0 : exceptionType.getName().hashCode());
			result = prime * result + ((sourceType == null) ? 0 : sourceType.getName().hashCode());

			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			HandlerKey other = (HandlerKey) obj;
			if (exceptionType == null) {
				if (other.exceptionType != null)
					return false;
			} else if (!exceptionType.equals(other.exceptionType))
				return false;
			if (sourceType == null) {
				if (other.sourceType != null)
					return false;
			} else if (!sourceType.equals(other.sourceType))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "HandlerKey [sourceType=" + sourceType + ", exceptionType=" + exceptionType + "]";
		}
	}

}
