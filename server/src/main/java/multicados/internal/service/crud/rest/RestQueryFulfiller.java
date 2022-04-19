/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.List;

import javax.persistence.EntityManager;

import multicados.internal.domain.DomainResource;
import multicados.internal.security.CredentialException;
import multicados.internal.service.crud.security.CRUDCredential;
import multicados.internal.service.crud.security.read.UnknownAttributesException;

/**
 * @author Ngoc Huy
 *
 */
public interface RestQueryFulfiller<TUPLE, EM extends EntityManager> {

	<D extends DomainResource> List<TUPLE> readAll(RestQuery<D> restQuery, CRUDCredential credential, EM entityManager)
			throws CredentialException, UnknownAttributesException, Exception;

}
