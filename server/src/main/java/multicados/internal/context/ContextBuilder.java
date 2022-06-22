/**
 * 
 */
package multicados.internal.context;

import javax.annotation.PostConstruct;

/**
 * @author Ngoc Huy
 *
 */
public interface ContextBuilder {

	default void summary() {};

	public static abstract class AbstractContextBuilder implements ContextBuilder {

		@PostConstruct
		public void doPostConstruct() {
			this.summary();
		}

	}

}
