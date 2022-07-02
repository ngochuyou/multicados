/**
 * 
 */
package multicados.internal.domain.metadata;

import multicados.internal.domain.DomainResource;

/**
 * A contract in describing a {@link DomainResource} type
 * 
 * @author Ngoc Huy
 *
 */
public interface DomainResourceMetadata<D extends DomainResource> {

	/**
	 * @return the {@link DomainResource} type which this metadata is describing
	 */
	Class<D> getResourceType();

	/**
	 * Locate an instance responsible for the requested role
	 * 
	 * @param <M>
	 * @return requested role
	 */
	<M extends DomainResourceMetadata<D>> M unwrap(Class<M> type);

}
