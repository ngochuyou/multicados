/**
 * 
 */
package multicados.internal.security;

import java.io.Serializable;

import multicados.internal.helper.FunctionHelper.HandledBiFunction;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractCredential<S extends Serializable> implements Credential<S> {

	@Override
	public <E extends S> Credential<E> and(Credential<E> next,
			HandledBiFunction<Credential<S>, Credential<E>, E, Exception> combiner) {
		return new CompositeCredential<>(this, next, combiner);
	}

	private class CompositeCredential<T extends S> extends AbstractCredential<T> {

		private final Credential<S> left;
		private final Credential<T> right;
		private final HandledBiFunction<Credential<S>, Credential<T>, T, Exception> combiner;

		public CompositeCredential(Credential<S> parent, Credential<T> child,
				HandledBiFunction<Credential<S>, Credential<T>, T, Exception> combiner) {
			super();
			this.left = parent;
			this.right = child;
			this.combiner = combiner;
		}

		@Override
		public T evaluate() throws CredentialException {
			try {
				return combiner.apply(left, right);
			} catch (Exception any) {
				throw new CredentialException(any);
			}
		}

		@Override
		public <E extends T> boolean has(Credential<E> target) {
			return left.has(target) || right.has(target);
		}

	}

}
