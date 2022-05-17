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
public class ComposedRestQueryImpl<D extends DomainResource> implements ComposedRestQuery<D> {

	private final RestQuery<D> delegatedQuery;

	private final List<ComposedNonBatchingRestQuery<?>> nonBatchingAssociationQueries;
	private final List<ComposedRestQuery<?>> batchingAssociationQueries;

	public ComposedRestQueryImpl(RestQuery<D> delegatedQuery,
			List<ComposedNonBatchingRestQuery<?>> nonBatchingAssociationQueries,
			List<ComposedRestQuery<?>> batchingAssociationQueries) {
		this.delegatedQuery = delegatedQuery;
		this.nonBatchingAssociationQueries = nonBatchingAssociationQueries;
		this.batchingAssociationQueries = batchingAssociationQueries;
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
	public Specification<D> getSpecification() {
		return delegatedQuery.getSpecification();
	}

	@Override
	public Pageable getPageable() {
		return delegatedQuery.getPageable();
	}

	@Override
	public void setPageable(Pageable pageable) {
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

}
