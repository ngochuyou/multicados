/**
 * 
 */
package multicados.internal.service;

import multicados.internal.domain.validation.Validation;
import multicados.internal.helper.Result;

/**
 * @author Ngoc Huy
 *
 */
public class ServiceResult extends Result {

	private Validation validation;
	private Exception exception;

	public ServiceResult(Status status) {
		super(status);
	}

	public Validation getValidation() {
		return validation;
	}

	public ServiceResult validation(Validation validation) {
		this.validation = validation;
		return this;
	}

	public Exception getException() {
		return exception;
	}

	public ServiceResult exception(Exception exception) {
		this.exception = exception;
		return this;
	}

	public static ServiceResult success() {
		return new ServiceResult(Status.SUCCESS);
	}

	public static ServiceResult failed(Exception exception) {
		return new ServiceResult(Status.SUCCESS).exception(exception);
	}

	public static ServiceResult bad(Validation validation) {
		return new ServiceResult(Status.SUCCESS).validation(validation);
	}

}
