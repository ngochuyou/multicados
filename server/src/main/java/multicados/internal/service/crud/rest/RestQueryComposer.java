/**
 *
 */
package multicados.internal.service.crud.rest;

import org.springframework.security.core.GrantedAuthority;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface RestQueryComposer {

	<D extends DomainResource> ComposedRestQuery<D> compose(RestQuery<D> restQuery, GrantedAuthority credential,
			boolean isBatched) throws Exception;

}
