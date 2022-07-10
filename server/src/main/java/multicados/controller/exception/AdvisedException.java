/**
 * 
 */
package multicados.controller.exception;

/**
 * @author Ngoc Huy
 *
 */
public class AdvisedException extends Throwable {

	private static final long serialVersionUID = 1L;

	private final Class<?> sourceType;

	public AdvisedException() {
		throw new UnsupportedOperationException();
	}

	public AdvisedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		throw new UnsupportedOperationException();
	}

	public AdvisedException(String message, Throwable cause) {
		throw new UnsupportedOperationException();
	}

	public AdvisedException(String message) {
		throw new UnsupportedOperationException();
	}

	public AdvisedException(Throwable cause) {
		throw new UnsupportedOperationException();
	}

	public AdvisedException(Class<?> sourceType, Throwable cause) {
		super(cause);
		this.sourceType = sourceType;
	}

	public Class<?> getSourceType() {
		return sourceType;
	}

}
