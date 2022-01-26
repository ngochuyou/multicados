/**
 * 
 */
package multicados.internal.domain;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.metadata.DomainResourceMetadata;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceContextProvider extends ContextBuilder {

	DomainResourceTree<DomainResource> getResourceTree();

	@SuppressWarnings("rawtypes")
	DomainResourceTree<Entity> getEntityTree();

	DomainResourceTree<Model> getModelTree();
	
	<T extends DomainResource> DomainResourceMetadata<T> getMetadata(Class<T> resourceType);

}
