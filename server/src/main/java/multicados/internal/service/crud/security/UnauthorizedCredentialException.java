/**
 * 
 */
package multicados.internal.service.crud.security;

import multicados.internal.security.CredentialException;

/**
 * @author Ngoc Huy
 *
 */
public class UnauthorizedCredentialException extends CredentialException {

	private static final long serialVersionUID = 1L;

	private static final String UNKNOWN_RESOURCE = "unknown resource";
	private static final String MESSAGE_TEMPLATE = "Unauthorized credential: %s on %s";

	private final String message;

	public UnauthorizedCredentialException(String deniedCredential) {
		this(deniedCredential, UNKNOWN_RESOURCE);
	}

	public UnauthorizedCredentialException(String deniedCredential, String resourceName) {
		super();
		message = String.format(MESSAGE_TEMPLATE, deniedCredential, resourceName);
	}

	@Override
	public String getMessage() {
		return message;
	}

}
