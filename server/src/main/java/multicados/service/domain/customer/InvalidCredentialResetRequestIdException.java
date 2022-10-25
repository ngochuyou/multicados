/**
 * 
 */
package multicados.service.domain.customer;

/**
 * @author Ngoc Huy
 *
 */
public class InvalidCredentialResetRequestIdException extends Throwable {

	private static final long serialVersionUID = 1L;

	public InvalidCredentialResetRequestIdException() {
		super();
	}

	public InvalidCredentialResetRequestIdException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidCredentialResetRequestIdException(String message) {
		super(message);
	}

	public InvalidCredentialResetRequestIdException(Throwable cause) {
		super(cause);
	}

}
