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
public interface Credential<S extends Serializable> {

	S evaluate() throws CredentialException;

	<E extends S> boolean has(Credential<E> target);

	<E extends S> Credential<E> and(Credential<E> next,
			HandledBiFunction<Credential<S>, Credential<E>, E, Exception> combiner);

}
