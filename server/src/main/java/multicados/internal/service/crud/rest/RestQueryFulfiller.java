/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.security.core.GrantedAuthority;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface RestQueryFulfiller<TUPLE, EM extends EntityManager> {

	<D extends DomainResource> List<TUPLE> readAll(RestQuery<D> restQuery, GrantedAuthority credential,
			EM entityManager) throws Exception;

	<D extends DomainResource> TUPLE read(RestQuery<D> restQuery, GrantedAuthority credential, EM entityManager)
			throws Exception;

}
