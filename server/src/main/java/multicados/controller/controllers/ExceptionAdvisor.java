/**
 * 
 */
package multicados.controller.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import multicados.internal.helper.Common;
import multicados.internal.helper.HttpHelper;
import multicados.internal.service.crud.security.UnauthorizedCredentialException;
import multicados.internal.service.crud.security.read.UnknownAttributesException;

/**
 * @author Ngoc Huy
 *
 */
@ControllerAdvice
public class ExceptionAdvisor extends ResponseEntityExceptionHandler {

	@ExceptionHandler(UnauthorizedCredentialException.class)
	public ResponseEntity<?> handledUnauthorizedCredentialException(UnauthorizedCredentialException ex,
			WebRequest request) {
		return checkForJsonOrText(ex, request, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(UnknownAttributesException.class)
	public ResponseEntity<?> handledUnknownAttributesException(UnknownAttributesException ex, WebRequest request) {
		return checkForJsonOrText(ex, request, HttpStatus.BAD_REQUEST);
	}

	private ResponseEntity<?> checkForJsonOrText(Exception ex, WebRequest request, HttpStatus status) {
		return handleExceptionInternal(ex,
				HttpHelper.isJsonAccepted(request) ? Common.error(ex.getMessage()) : ex.getMessage(), HttpHeaders.EMPTY,
				status, request);
	}

}
