/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.List;

import javax.persistence.EntityManager;

import multicados.internal.domain.DomainResource;
import multicados.internal.service.crud.security.CRUDCredential;

/**
 * @author Ngoc Huy
 *
 */
public interface RestQueryFulfiller<TUPLE, EM extends EntityManager> {

	/*
	 * <D extends DomainResource> List<TUPLE> readAll(BatchingRestQuery<D>
	 * restQuery, CRUDCredential credential, EM entityManager) throws
	 * CredentialException, UnknownAttributesException, Exception;
	 * 
	 * <D extends DomainResource> TUPLE read(NonBatchingRestQuery<D> restQuery,
	 * CRUDCredential credential, EM entityManager) throws CredentialException,
	 * UnknownAttributesException, Exception;
	 */

	<D extends DomainResource> List<TUPLE> readAll(RestQuery<D> restQuery, CRUDCredential credential, EM entityManager)
			throws Exception;

	<D extends DomainResource> TUPLE read(RestQuery<D> restQuery, CRUDCredential credential, EM entityManager)
			throws Exception;

}
