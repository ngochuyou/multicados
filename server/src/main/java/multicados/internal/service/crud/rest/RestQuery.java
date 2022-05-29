/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.List;

import org.springframework.data.domain.Pageable;

import multicados.internal.domain.DomainResource;
import multicados.internal.service.crud.rest.AbstractRestQuery.PageableImpl;

/**
 * @author Ngoc Huy
 *
 */
public interface RestQuery<D extends DomainResource> {

	Class<D> getResourceType();

	List<String> getAttributes();

	void setAttributes(List<String> attributes);

	PageableImpl getPage();

	void setPage(Pageable pageable);

	String getAssociationName();

	void setAssociationName(String name);

}
