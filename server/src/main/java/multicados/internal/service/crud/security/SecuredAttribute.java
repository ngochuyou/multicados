/**
 * 
 */
package multicados.internal.service.crud.security;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface SecuredAttribute<D extends DomainResource> {

	Class<D> getOwningType();

	CRUDCredential getCredential();

	String getName();

	String getAlias();

	boolean isMasked();

}
