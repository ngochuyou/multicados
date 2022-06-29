/**
 *
 */
package multicados.internal.service.crud.rest;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface ComposedNonBatchingRestQuery<D extends DomainResource> extends ComposedRestQuery<D> {

	Integer getAssociatedPosition();

	int getPropertySpan();

}
