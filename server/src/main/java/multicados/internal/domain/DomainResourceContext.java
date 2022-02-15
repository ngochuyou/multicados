/**
 * 
 */
package multicados.internal.domain;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.tuplizer.DomainResourceTuplizer;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceContext extends ContextBuilder {

	DomainResourceGraph<DomainResource> getResourceGraph();

	@SuppressWarnings("rawtypes")
	DomainResourceGraph<Entity> getEntityGraph();

	<T extends DomainResource> DomainResourceMetadata<T> getMetadata(Class<T> resourceType);

	<T extends DomainResource> DomainResourceTuplizer<T> getTuplizer(Class<T> resourceType);

}
