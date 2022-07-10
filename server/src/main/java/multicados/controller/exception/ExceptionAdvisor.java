/**
 * 
 */
package multicados.controller.exception;

import java.util.function.BiFunction;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import multicados.internal.helper.Utils.BiDeclaration;

/**
 * An {@link ControllerAdvice} contract that exposes it's exception handle
 * strategies to be contributed
 * 
 * @author Ngoc Huy
 *
 */
public interface ExceptionAdvisor {

	/**
	 * Contribute an exception handler
	 * 
	 * @param <T>
	 * @param sourceType    the source from which the handler originates
	 * @param exceptionType type of the exception
	 * @param advisor       the contributed handler
	 * @throws IllegalAccessException
	 */
	<T extends Throwable> void register(Class<?> sourceType, Class<T> exceptionType,
			BiFunction<T, WebRequest, BiDeclaration<HttpStatus, Object>> advisor) throws IllegalAccessException;

}
