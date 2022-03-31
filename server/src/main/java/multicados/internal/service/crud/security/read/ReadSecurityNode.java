/**
 * 
 */
package multicados.internal.service.crud.security.read;

import java.util.Collection;
import java.util.List;

import multicados.internal.domain.DomainResource;
import multicados.internal.security.CredentialException;
import multicados.internal.service.crud.security.CRUDCredential;
import multicados.internal.service.crud.security.UnauthorizedCredentialException;

/**
 * @author Ngoc Huy
 *
 */
public interface ReadSecurityNode<D extends DomainResource> {

	List<String> check(Collection<String> requestedAttributes, CRUDCredential credential)
			throws CredentialException, UnknownAttributesException, UnauthorizedCredentialException;

}
