/**
 * 
 */
package multicados.internal.domain.validation;

import java.io.Serializable;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.GraphWalker;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceValidator<T extends DomainResource> extends GraphWalker<T> {

	Validation isSatisfiedBy(T resource);

	Validation isSatisfiedBy(Serializable id, T resource);

	<E extends T> DomainResourceValidator<E> and(DomainResourceValidator<E> next);

	@Override
	default <E extends T> GraphWalker<E> and(GraphWalker<E> next) {
		return and((DomainResourceValidator<E>) next);
	}
	
}
