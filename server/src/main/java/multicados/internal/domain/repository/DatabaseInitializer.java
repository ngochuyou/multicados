/**
 *
 */
package multicados.internal.domain.repository;

import multicados.internal.context.ContextBuilder;

/**
 * @author Ngoc Huy
 *
 */
public interface DatabaseInitializer extends ContextBuilder {

	public interface DatabaseInitializerContributor {

		void contribute() throws Exception;

	}

}
