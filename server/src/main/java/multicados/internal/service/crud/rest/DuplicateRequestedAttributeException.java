/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.Collection;

import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
public class DuplicateRequestedAttributeException extends Exception {

	private static final long serialVersionUID = 1L;

	private final String message;

	public DuplicateRequestedAttributeException(Collection<String> collidedAttributes) {
		message = String.format("Following attributes were requested duplicately: [%s]",
				StringHelper.join(collidedAttributes));
	}

	@Override
	public String getMessage() {
		return message;
	}

}
