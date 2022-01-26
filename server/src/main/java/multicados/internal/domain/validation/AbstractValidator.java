/**
 * 
 */
package multicados.internal.domain.validation;

import java.io.Serializable;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractValidator<T extends DomainResource> implements Validator<T> {

	@Override
	public <E extends T> Validator<E> and(Validator<E> next) {
		return new CompositeValidator<>(this, next);
	}

	private class CompositeValidator<E extends T> extends AbstractValidator<E> {

		private final Validator<T> left;
		private final Validator<E> right;

		private CompositeValidator(Validator<T> left, Validator<E> right) {
			this.left = left;
			this.right = right;
		}

		@Override
		public Validation isSatisfiedBy(E resource) {
			return right.isSatisfiedBy(resource).and(left.isSatisfiedBy(resource));
		}

		@Override
		public Validation isSatisfiedBy(Serializable id, E resource) {
			return right.isSatisfiedBy(id, resource).and(left.isSatisfiedBy(id, resource));
		}

		@Override
		public String getLoggableName() {
			return String.format("%s, %s", left.getLoggableName(), right.getLoggableName());
		}

	}

}
