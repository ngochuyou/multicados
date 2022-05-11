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
public interface ComposedRestQuery<D extends DomainResource> extends RestQuery<D> {

	List<ComposedRestQuery<?>> getBatchingAssociationQueries();

	List<ComposedNonBatchingRestQuery<?>> getNonBatchingAssociationQueries();

}
