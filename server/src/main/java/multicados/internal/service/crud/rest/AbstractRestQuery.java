/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.List;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
/**
 * @author Ngoc Huy
 *
 * @param <D>
 */
public abstract class AbstractRestQuery<D extends DomainResource> implements RestQuery<D> {

	private final Class<D> resourceType;
	
	private List<String> columns;

	public AbstractRestQuery(Class<D> resourceType) {
		this.resourceType = resourceType;
	}

	public Class<D> getResourceType() {
		return resourceType;
	}

	public List<String> getColumns() {
		return columns;
	}

	public void setColumns(List<String> columns) {
		this.columns = columns;
	}

}
