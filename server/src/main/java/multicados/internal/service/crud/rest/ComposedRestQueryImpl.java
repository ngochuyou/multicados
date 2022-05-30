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
public class ComposedRestQueryImpl<D extends DomainResource> implements ComposedRestQuery<D> {

	private final RestQuery<D> delegatedQuery;

	private final List<ComposedNonBatchingRestQuery<?>> nonBatchingAssociationQueries;
	private final List<ComposedRestQuery<?>> batchingAssociationQueries;
	private final Map<String, Filter<?>> filtersMap;

	public ComposedRestQueryImpl(RestQuery<D> delegatedQuery,
			List<ComposedNonBatchingRestQuery<?>> nonBatchingAssociationQueries,
			List<ComposedRestQuery<?>> batchingAssociationQueries, Map<String, Filter<?>> filtersMap) {
		this.delegatedQuery = delegatedQuery;
		this.nonBatchingAssociationQueries = nonBatchingAssociationQueries;
		this.batchingAssociationQueries = batchingAssociationQueries;
		this.filtersMap = filtersMap;
	}

	@Override
	public Class<D> getResourceType() {
		return delegatedQuery.getResourceType();
	}

	@Override
	public List<String> getAttributes() {
		return delegatedQuery.getAttributes();
	}

	@Override
	public void setAttributes(List<String> attributes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DelegatedPageable getPage() {
		return delegatedQuery.getPage();
	}

	@Override
	public void setPage(DelegatedPageable pageable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAssociationName() {
		return delegatedQuery.getAssociationName();
	}

	@Override
	public void setAssociationName(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<ComposedRestQuery<?>> getBatchingAssociationQueries() {
		return batchingAssociationQueries;
	}

	@Override
	public List<ComposedNonBatchingRestQuery<?>> getNonBatchingAssociationQueries() {
		return nonBatchingAssociationQueries;
	}

	@Override
	public Map<String, Filter<?>> getFilters() {
		return filtersMap;
	}

}
