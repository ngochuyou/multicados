/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public class ComposedNonBatchingRestQueryImpl<D extends DomainResource> extends ComposedRestQueryImpl<D>
		implements ComposedNonBatchingRestQuery<D> {

	private final Integer associatedPosition;
	private final int span;

	public ComposedNonBatchingRestQueryImpl(RestQuery<D> delegatedQuery,
			List<ComposedNonBatchingRestQuery<?>> nonBatchingAssociationQueries,
			List<ComposedRestQuery<?>> batchingAssociationQueries, Integer associatedPosition) {
		super(delegatedQuery, nonBatchingAssociationQueries, batchingAssociationQueries);

		this.associatedPosition = associatedPosition;
		span = delegatedQuery.getAttributes().size() + getSizeOrZero(nonBatchingAssociationQueries)
				+ getSizeOrZero(batchingAssociationQueries);
	}

	private int getSizeOrZero(List<?> collection) {
		return Optional.ofNullable(collection).map(Collection::size).orElse(0);
	}

	@Override
	public Integer getAssociatedPosition() {
		return associatedPosition;
	}

	@Override
	public int getPropertySpan() {
		return span;
	}

}
