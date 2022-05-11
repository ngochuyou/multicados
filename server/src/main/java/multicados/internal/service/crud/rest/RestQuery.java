/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface RestQuery<D extends DomainResource> {

	Class<D> getResourceType();

	List<String> getAttributes();

	void setAttributes(List<String> attributes);

	Specification<D> getSpecification();

	Pageable getPageable();

	void setPageable(Pageable pageable);

	String getName();
	
	void setName(String name);
	
}
