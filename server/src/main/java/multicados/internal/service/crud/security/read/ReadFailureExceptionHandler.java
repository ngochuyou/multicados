/**
 * 
 */
package multicados.internal.service.crud.security.read;

import java.util.Collection;
import java.util.List;

import multicados.internal.security.CredentialException;

/**
 * @author Ngoc Huy
 *
 */
public interface ReadFailureExceptionHandler {

	void doOnUnauthorizedCredential(Class<?> resourceType, String credential) throws CredentialException;

	List<String> doOnInvalidAttributes(Class<?> resourceType, Collection<String> requestedAttributes,
			Collection<String> authorizedAttributes) throws UnknownAttributesException;

}
