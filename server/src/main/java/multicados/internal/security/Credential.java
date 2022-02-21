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

	boolean has(Credential<S> target);

	Credential<S> and(Credential<S> next, HandledBiFunction<Credential<S>, Credential<S>, S, Exception> combiner);

}
