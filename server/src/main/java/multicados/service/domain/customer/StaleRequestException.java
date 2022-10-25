/**
 * 
 */
package multicados.service.domain.customer;

/**
 * @author Ngoc Huy
 *
 */
public class StaleRequestException extends Throwable {

	private static final long serialVersionUID = 1L;

	public StaleRequestException() {
		super();
	}

	public StaleRequestException(String message) {
		super(message);
	}

	public StaleRequestException(Throwable cause) {
		super(cause);
	}

}
