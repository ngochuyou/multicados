/**
 * 
 */
package multicados.internal.service.crud.security.read;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.DomainResource;
import multicados.internal.security.CredentialException;

/**
 * @author Ngoc Huy
 *
 */
public interface ReadSecurityManager extends ContextBuilder {

	<D extends DomainResource> List<String> check(Class<D> resourceType, Collection<String> requestedAttributes,
			GrantedAuthority credential) throws CredentialException, UnknownAttributesException;

	<D extends DomainResource> Map<String, String> translate(Class<D> resourceType, Collection<String> attributes);

	interface WithType<D extends DomainResource> {

		WithCredential<D> credentials(GrantedAuthority... credentials);

	}

	interface WithCredential<D extends DomainResource> {

		WithAttribute<D> attributes(String... attributes);

		WithCredential<D> credentials(GrantedAuthority credential);

		WithCredential<D> mask();

		WithCredential<D> publish();

		<E extends DomainResource> WithType<E> type(Class<E> type);

	}

	interface WithAttribute<D extends DomainResource> {

		WithAttribute<D> use(String... alias);

		WithAttribute<D> publish();

		WithAttribute<D> mask();

		WithAttribute<D> others();

		WithAttribute<D> attributes(String... attributes);

		WithCredential<D> credentials(GrantedAuthority... credentials);

		<E extends DomainResource> WithType<E> type(Class<E> type);

	}

}
