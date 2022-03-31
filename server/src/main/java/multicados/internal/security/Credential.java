/**
 * 
 */
package multicados.internal.security;

import java.io.Serializable;

/**
 * @author Ngoc Huy
 *
 */
public interface Credential<S extends Serializable> {

	S evaluate() throws CredentialException;

}
