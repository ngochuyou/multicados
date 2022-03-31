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

	private static final String UNKNOWN_RESOURCE = "unknown resource";
	private static final String MESSAGE_TEMPLATE = "One or many of requested attributes %s in resource %s is/are unknown";

	private final String message;

	public UnknownAttributesException(Collection<String> attributeNames) {
		this(attributeNames, UNKNOWN_RESOURCE);
	}

	public UnknownAttributesException(Collection<String> attributeNames, String resourceName) {
		super();
		message = String.format(MESSAGE_TEMPLATE, StringHelper.join(attributeNames), resourceName);
	}

	@Override
	public String getMessage() {
		return message;
	}

}
