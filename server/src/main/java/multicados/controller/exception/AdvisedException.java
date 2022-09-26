/**
 * 
 */
package multicados.controller.exception;

/**
 * @author Ngoc Huy
 *
 */
public class AdvisedException extends Exception {

	private static final long serialVersionUID = 1L;

	private final Class<?> sourceType;

	public AdvisedException(Throwable cause) {
		this(cause.getClass(), cause);
	}

	public AdvisedException(Class<?> sourceType, Throwable cause) {
		super(cause);
		this.sourceType = sourceType;
	}

	public Class<?> getSourceType() {
		return sourceType;
	}

}
