/**
 * 
 */
package multicados.internal.service.crud.security;

import multicados.internal.security.CompositeCredential;

/**
 * @author Ngoc Huy
 *
 */
public interface CRUDCredential extends CompositeCredential<String> {

	String getDelimiter();

}
