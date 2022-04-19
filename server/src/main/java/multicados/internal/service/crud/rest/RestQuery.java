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
	
	boolean isEmpty();
	
	List<String> getColumns();
	
	List<RestQuery<?>> getAssociations();
	
}
