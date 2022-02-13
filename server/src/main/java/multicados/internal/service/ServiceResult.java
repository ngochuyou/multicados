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

	public ServiceResult(Status status) {
		super(status);
	}

	public Validation getValidation() {
		return validation;
	}

	public void setValidation(Validation validation) {
		this.validation = validation;
	}

}
