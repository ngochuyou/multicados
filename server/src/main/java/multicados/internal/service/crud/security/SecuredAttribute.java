/**
 * 
 */
package multicados.internal.service.crud.security;

import org.springframework.security.core.GrantedAuthority;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface SecuredAttribute<D extends DomainResource> {

	Class<D> getOwningType();

	GrantedAuthority getCredential();

	String getName();

	String getAlias();

	boolean isMasked();

}
