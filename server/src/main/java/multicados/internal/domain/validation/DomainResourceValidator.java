/**
 *
 */
package multicados.internal.domain.validation;

import java.io.Serializable;

import javax.persistence.EntityManager;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.GraphLogic;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceValidator<T extends DomainResource> extends GraphLogic<T> {

	Validation isSatisfiedBy(EntityManager entityManager, T resource) throws Exception;

	Validation isSatisfiedBy(EntityManager entityManager, Serializable id, T resource) throws Exception;

	<E extends T> DomainResourceValidator<E> and(DomainResourceValidator<E> next);

	@Override
	default <E extends T> GraphLogic<E> and(GraphLogic<E> next) {
		return and((DomainResourceValidator<E>) next);
	}

}
