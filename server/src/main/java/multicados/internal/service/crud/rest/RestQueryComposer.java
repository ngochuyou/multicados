/**
 * 
 */
package multicados.internal.service.crud.rest;

import multicados.internal.domain.DomainResource;
import multicados.internal.service.crud.security.CRUDCredential;

/**
 * @author Ngoc Huy
 *
 */
public interface RestQueryComposer {

	<D extends DomainResource> ComposedRestQuery<D> compose(RestQuery<D> restQuery, CRUDCredential credential,
			boolean isBatched) throws Exception;

}
