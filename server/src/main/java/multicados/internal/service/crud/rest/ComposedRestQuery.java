/**
 *
 */
package multicados.internal.service.crud.rest;

import java.util.List;
import java.util.Map;

import multicados.internal.domain.DomainResource;
import multicados.internal.service.crud.rest.filter.Filter;

/**
 * @author Ngoc Huy
 *
 */
public interface ComposedRestQuery<D extends DomainResource> extends RestQuery<D> {

	List<ComposedRestQuery<?>> getBatchingAssociationQueries();

	List<ComposedNonBatchingRestQuery<?>> getNonBatchingAssociationQueries();

	Map<String, Filter<?>> getFilters();

}
