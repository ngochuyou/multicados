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

	private List<String> attributes;
	private DelegatedPageable page;

	private String associationName;

	public AbstractRestQuery(Class<D> resourceType) {
		this.resourceType = resourceType;
	}

	@Override
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
	public DelegatedPageable getPage() {
		return page;
	}

	@Override
	public void setPage(DelegatedPageable pageable) {
		this.page = pageable;
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
