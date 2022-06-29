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
public interface RestQuery<D extends DomainResource> {

	Class<D> getResourceType();

	List<String> getAttributes();

	void setAttributes(List<String> attributes);

	DelegatedPageable getPage();

	void setPage(DelegatedPageable pageable);

	String getAssociationName();

	void setAssociationName(String name);

}
