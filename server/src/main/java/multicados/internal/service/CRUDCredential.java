/**
 * 
 */
package multicados.internal.service;

import multicados.internal.security.Credential;

/**
 * @author Ngoc Huy
 *
 */
public interface CRUDCredential extends Credential<String> {

	String getDelimiter();

	Credential<String> and(Credential<String> next);

}
