/**
 * 
 */
package multicados.internal.context;

/**
 * @author Ngoc Huy
 *
 */
public interface Loggable {

	default String getLoggableName() {
		return this.getClass().getSimpleName();
	}

}
