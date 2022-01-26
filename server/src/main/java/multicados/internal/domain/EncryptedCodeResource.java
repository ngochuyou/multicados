/**
 * 
 */
package multicados.internal.domain;

import java.io.Serializable;

/**
 * @author Ngoc Huy
 *
 */
public interface EncryptedCodeResource {

	Serializable getId();

	void setCode(String encrypted);

	String code_ = "code";

}
