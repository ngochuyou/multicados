/**
 * 
 */
package multicados.internal.domain.metadata;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractDomainResourceMetadata<D extends DomainResource> implements DomainResourceMetadata<D> {

	private final Class<D> resourceType;

	public AbstractDomainResourceMetadata(Class<D> resourceType) {
		this.resourceType = resourceType;
	}

	@Override
	public Class<D> getResourceType() {
		return resourceType;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <M extends DomainResourceMetadata<D>> M unwrap(Class<M> type) {
		return (M) this;
	}

}
