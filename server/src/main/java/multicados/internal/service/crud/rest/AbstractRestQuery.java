/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.List;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 * @param <D>
 */
public abstract class AbstractRestQuery<D extends DomainResource> implements RestQuery<D> {

	private final Class<D> resourceType;

	private List<String> properties;

	public AbstractRestQuery(Class<D> resourceType) {
		this.resourceType = resourceType;
	}

	public Class<D> getResourceType() {
		return resourceType;
	}

	@Override
	public List<String> getProperties() {
		return properties;
	}

	public void setProperties(List<String> properties) {
		this.properties = properties;
	}

}
