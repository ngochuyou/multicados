/**
 * 
 */
package multicados.internal.security;

import java.io.Serializable;

import multicados.internal.context.ContextBuilder;

/**
 * @author Ngoc Huy
 *
 */
public interface CredentialFactory extends ContextBuilder {

	<S extends Serializable> Credential<S> make(S evaluation);
	
	<S extends Serializable> Credential<S> getUnknown();
	
}
