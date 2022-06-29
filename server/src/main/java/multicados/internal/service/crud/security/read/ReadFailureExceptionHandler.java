/**
 *
 */
package multicados.internal.service.crud.security.read;

import java.util.List;

import multicados.internal.security.CredentialException;

/**
 * @author Ngoc Huy
 *
 */
public interface ReadFailureExceptionHandler {

	void doOnUnauthorizedCredential(Class<?> resourceType, String credential) throws CredentialException;

	void doOnUnauthorizedAttribute(Class<?> resourceType, String credential, List<String> unauthorizedAttributeNames) throws UnknownAttributesException;

}
