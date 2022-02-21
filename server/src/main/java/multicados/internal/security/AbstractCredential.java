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
	public Credential<S> and(Credential<S> next,
			HandledBiFunction<Credential<S>, Credential<S>, S, Exception> combiner) {
		return new CompositeCredential(this, next, combiner);
	}

	private class CompositeCredential extends AbstractCredential<S> {

		private final Credential<S> left;
		private final Credential<S> right;
		private final HandledBiFunction<Credential<S>, Credential<S>, S, Exception> combiner;

		public CompositeCredential(Credential<S> parent, Credential<S> child,
				HandledBiFunction<Credential<S>, Credential<S>, S, Exception> combiner) {
			super();
			this.left = parent;
			this.right = child;
			this.combiner = combiner;
		}

		@Override
		public S evaluate() throws CredentialException {
			try {
				return combiner.apply(left, right);
			} catch (Exception any) {
				throw new CredentialException(any);
			}
		}

		@Override
		public boolean has(Credential<S> target) {
			return left.has(target) || right.has(target);
		}

	}

}
