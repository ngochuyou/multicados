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
	
	String getName();

	List<String> getProperties();

	List<RestQuery<?>> getQueries();

	Specification<D> getSpecification();
	
	Pageable getPageable();

}
