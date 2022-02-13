/**
 * 
 */
package multicados.internal.domain;

import java.io.Serializable;

/**
 * @author Ngoc Huy
 *
 */
public interface EncryptedIdentifierResource<S extends Serializable> extends IdentifiableDomainResource<S> {

	void setCode(String encrypted);

	String code_ = "code";

}
