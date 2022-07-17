/**
 *
 */
package multicados.controller.controllers;

import static org.springframework.http.ResponseEntity.status;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;

import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.Common;
import multicados.internal.helper.HttpHelper;
import multicados.internal.service.ServiceResult;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(AbstractController.class);

	protected static final String HEAD = "HEAD";

	/* ========================================================== */

	/**
	 * Construct a {@link ResponseEntity} body based on the
	 * {@link HttpHeaders.CONTENT_TYPE} with 404 code
	 * 
	 * @param preficies the message which is included in the body
	 * @param request
	 *
	 * @return the {@link ResponseEntity}
	 */
	protected ResponseEntity<?> sendNotFound(Collection<String> preficies, HttpServletRequest request) {
		return doSendWithError((BodyBuilder) ResponseEntity.notFound(), Common.notFound(preficies), request);
	}

	/**
	 * Construct a {@link ResponseEntity} body based on the
	 * {@link HttpHeaders.CONTENT_TYPE} with 200 code
	 * 
	 * @param body    the data to be included in the response body
	 * @param request
	 *
	 * @param <T>     body type
	 * @return
	 */
	protected <T> ResponseEntity<?> sendOk(T body, HttpServletRequest request) {
		return doSendWithPayload(ResponseEntity.ok(), body, request);
	}

	/**
	 * Construct a {@link ResponseEntity} body based on the
	 * {@link HttpHeaders.CONTENT_TYPE} with 400 code
	 * 
	 * @param request
	 * @param message the data to be included in the response body
	 *
	 * @param <T>     body type
	 * @return
	 */
	protected <T> ResponseEntity<?> sendBad(String message, HttpServletRequest request) {
		return doSendWithError(ResponseEntity.badRequest(), message, request);
	}

	/**
	 * Construct a {@link ResponseEntity} body based on the
	 * {@link HttpHeaders.CONTENT_TYPE} with 403 code
	 * 
	 * @param request
	 * @param message the data to be included in the response body
	 *
	 * @param <T>     body type
	 * @return
	 */
	protected <T> ResponseEntity<?> sendForbidden(String message, HttpServletRequest request) {
		return doSendWithError(ResponseEntity.status(HttpStatus.FORBIDDEN), message, request);
	}

	protected <T> ResponseEntity<?> sendResult(ServiceResult result, T body, HttpServletRequest request)
			throws Exception {
		if (result.isOk()) {
			return sendOk(body, request);
		}

		if (result.getException() != null) {
			if (logger.isErrorEnabled()) {
				logger.error("Exception encountered: {}", result.getException().getMessage());
			}

			throw result.getException();
		}
		// @formatter:off
		return status(HttpStatus.BAD_REQUEST)
				.body(result.getValidation().getErrors().entrySet().stream()
					.map(entry -> Map.entry(entry.getKey(), entry.getValue().getMessage()))
					.collect(CollectionHelper.toMap()));
		// @formatter:on
	}

	protected ResponseEntity<?> doSendWithError(BodyBuilder responseBuilder, Object body, HttpServletRequest request) {
		return doSend(responseBuilder, () -> Common.error(body), request);
	}

	protected ResponseEntity<?> doSendWithPayload(BodyBuilder responseBuilder, Object body,
			HttpServletRequest request) {
		return doSend(responseBuilder, () -> Common.payload(body), request);
	}

	private ResponseEntity<?> doSend(BodyBuilder responseBuilder, Supplier<Object> bodySupplier,
			HttpServletRequest request) {
		if (HttpHelper.isJsonAccepted(request)) {
			return responseBuilder.body(bodySupplier.get());
		}

		if (HttpHelper.isTextAccepted(request)) {
			return responseBuilder.body(bodySupplier.get().toString());
		}

		return responseBuilder.body(null);
	}

	/* ========================================================== */

	protected Session useManualSession(Session session) {
		session.setHibernateFlushMode(FlushMode.MANUAL);
		return session;
	}

	protected Session useAutoSession(Session session) {
		session.setHibernateFlushMode(FlushMode.AUTO);
		return session;
	}

}
