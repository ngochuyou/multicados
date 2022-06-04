/**
 * 
 */
package multicados.internal.service.crud.security.read;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;

import multicados.internal.domain.DomainResource;
import multicados.internal.security.CredentialException;

/**
 * @author Ngoc Huy
 *
 */
public interface ReadSecurityNode<D extends DomainResource> {

	List<String> check(Collection<String> requestedAttributes, GrantedAuthority credential)
			throws CredentialException, UnknownAttributesException;

	Map<String, String> translate(Collection<String> attributes);

}
