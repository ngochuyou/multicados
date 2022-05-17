/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.List;

import org.springframework.data.domain.Pageable;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 * @param <D>
 */
public abstract class AbstractRestQuery<D extends DomainResource> implements RestQuery<D> {

	private final Class<D> resourceType;

	private List<String> attributes;
	private Pageable pageable;

	private String associationName;

	public AbstractRestQuery(Class<D> resourceType) {
		this.resourceType = resourceType;
	}

	public Class<D> getResourceType() {
		return resourceType;
	}

	@Override
	public List<String> getAttributes() {
		return attributes;
	}

	@Override
	public void setAttributes(List<String> attributes) {
		this.attributes = attributes;
	}

	@Override
	public Pageable getPageable() {
		return pageable;
	}

	@Override
	public void setPageable(Pageable pageable) {
		this.pageable = pageable;
	}

	@Override
	public String getAssociationName() {
		return associationName;
	}

	@Override
	public void setAssociationName(String associationName) {
		this.associationName = associationName;
	}

}
