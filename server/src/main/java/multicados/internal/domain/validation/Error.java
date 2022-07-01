/**
 *
 */
package multicados.internal.domain.validation;

/**
 * @author Ngoc Huy
 *
 */
public class Error {

	private final Integer code;
	private final String message;

	public Error(Integer code, String message) {
		this.code = code;
		this.message = message;
	}

	public Integer getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

}
