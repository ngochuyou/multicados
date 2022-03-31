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
public abstract class AbstractCompositeCredential<S extends Serializable> implements CompositeCredential<S> {

	@Override
	public CompositeCredential<S> and(Credential<S> next) {
		return new CompositeCredentialImpl(this, next, getCombiner());
	}

	protected abstract HandledBiFunction<Credential<S>, Credential<S>, S, CredentialException> getCombiner();

	private class CompositeCredentialImpl extends AbstractCompositeCredential<S> {

		private final Credential<S> left;
		private final Credential<S> right;
		private final HandledBiFunction<Credential<S>, Credential<S>, S, CredentialException> combiner;

		public CompositeCredentialImpl(Credential<S> left, Credential<S> right,
				HandledBiFunction<Credential<S>, Credential<S>, S, CredentialException> combiner) {
			this.left = left;
			this.right = right;
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
		public boolean has(Credential<S> candidate) {
			if (CompositeCredential.class.isAssignableFrom(left.getClass())) {
				if (((CompositeCredential<S>) left).has(candidate)) {
					return true;
				}
			}

			if (CompositeCredential.class.isAssignableFrom(right.getClass())) {
				if (((CompositeCredential<S>) right).has(candidate)) {
					return true;
				}
			}

			if (left.equals(candidate) || right.equals(candidate)) {
				return true;
			}

			return false;
		}

		@Override
		protected HandledBiFunction<Credential<S>, Credential<S>, S, CredentialException> getCombiner() {
			return combiner;
		}

	}

}
