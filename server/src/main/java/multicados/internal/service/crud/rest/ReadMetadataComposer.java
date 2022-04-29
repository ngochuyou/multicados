/**
 * 
 */
package multicados.internal.service.crud.rest;

import multicados.internal.domain.DomainResource;
import multicados.internal.security.CredentialException;
import multicados.internal.service.crud.security.CRUDCredential;
import multicados.internal.service.crud.security.read.UnknownAttributesException;

/**
 * @author Ngoc Huy
 *
 */
public interface ReadMetadataComposer {

	<D extends DomainResource> ReadMetadata<D> compose(RestQuery<D> restQuery, CRUDCredential credential)
			throws CredentialException, UnknownAttributesException;

}
