/**
 *
 */
package multicados.internal.service.crud.security.read;

import java.util.Collection;

import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
public class UnknownAttributesException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String MESSAGE_TEMPLATE = "One or many of requested attributes: [%s] is/are unknown";

	private final String message;

	public UnknownAttributesException(Collection<String> attributeNames) {
		super();
		message = String.format(MESSAGE_TEMPLATE, StringHelper.join(attributeNames));
	}

	@Override
	public String getMessage() {
		return message;
	}

}
