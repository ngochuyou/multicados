/**
 *
 */
package multicados.internal.domain;

import java.io.Serializable;

/**
 * @author Ngoc Huy
 *
 */
public interface EncryptedIdentifierResource<S extends Serializable> extends IdentifiableResource<S> {

	void setCode(String encrypted);

}
