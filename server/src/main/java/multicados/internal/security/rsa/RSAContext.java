/**
 * 
 */
package multicados.internal.security.rsa;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * RSA security management, responsibility for resolving and providing a
 * {@link PrivateKey} and {@link PublicKey}.
 * 
 * @author Ngoc Huy
 *
 */
public interface RSAContext {

	/**
	 * @return the {@link PrivateKey} managed by this context
	 */
	PrivateKey getPrivateKey();

	/**
	 * @return the {@link PublicKey} managed by this context
	 */
	PublicKey getPublicKey();

}
