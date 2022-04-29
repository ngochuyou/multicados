/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import multicados.internal.domain.DomainResource;
import multicados.internal.service.crud.security.CRUDCredential;

/**
 * @author Ngoc Huy
 *
 */
public interface ReadMetadata<D extends DomainResource> {

	Class<D> getResourceType();
	
	List<String> getAttributes();

	List<ReadMetadata<?>> getMetadatas();

	CRUDCredential getCredential();
	
	Specification<D> getSpecification();
	
	String getName();

}
