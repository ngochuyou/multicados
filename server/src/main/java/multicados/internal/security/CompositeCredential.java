/**
 * 
 */
package multicados.internal.security;

import java.io.Serializable;

/**
 * @author Ngoc Huy
 *
 */
public interface CompositeCredential<S extends Serializable> extends Credential<S> {

	CompositeCredential<S> and(Credential<S> next);

	boolean has(Credential<S> candidate);

}
