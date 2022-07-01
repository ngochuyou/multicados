/**
 *
 */
package multicados.internal.domain.validation;

import java.io.Serializable;

import javax.persistence.EntityManager;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractDomainResourceValidator<T extends DomainResource> implements DomainResourceValidator<T> {

	@Override
	public <E extends T> DomainResourceValidator<E> and(DomainResourceValidator<E> next) {
		return new CompositeValidator<>(this, next);
	}

	private class CompositeValidator<E extends T> extends AbstractDomainResourceValidator<E> {

		private final DomainResourceValidator<T> left;
		private final DomainResourceValidator<E> right;

		private CompositeValidator(DomainResourceValidator<T> left, DomainResourceValidator<E> right) {
			this.left = left;
			this.right = right;
		}

		@Override
		public Validation isSatisfiedBy(EntityManager entityManager, E resource) throws Exception {
			return isSatisfiedBy(entityManager, null, resource);
		}

		@Override
		public Validation isSatisfiedBy(EntityManager entityManager, Serializable id, E resource) throws Exception {
			return right.isSatisfiedBy(entityManager, id, resource)
					.and(left.isSatisfiedBy(entityManager, id, resource));
		}

		@Override
		public String getLoggableName() {
			return String.format("[%s, %s]", left.getLoggableName(), right.getLoggableName());
		}

	}

}
