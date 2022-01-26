/**
 * 
 */
package multicados.internal.domain.builder;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceBuilderFactory extends ContextBuilder {

	<E extends DomainResource, T extends DomainResourceBuilder<E>> DomainResourceBuilder<E> getBuilder(
			Class<E> resourceClass);

}
