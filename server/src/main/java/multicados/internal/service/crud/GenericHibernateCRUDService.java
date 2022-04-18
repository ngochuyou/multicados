/**
 * 
 */
package multicados.internal.service.crud;

import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.DomainResource;
import multicados.internal.service.crud.security.CRUDCredential;

/**
 * @author Ngoc Huy
 *
 */
public interface GenericHibernateCRUDService<TUPLE> extends GenericCRUDService<TUPLE, Session>, ContextBuilder {

	<E extends DomainResource> List<TUPLE> readAll(Class<E> type, Collection<String> properties,
			Specification<E> specification, Pageable pageable, CRUDCredential credential, Session entityManager)
			throws Exception;

	<E extends DomainResource> List<TUPLE> readAll(Class<E> type, Collection<String> properties,
			Specification<E> specification, CRUDCredential credential, Session entityManager) throws Exception;

}
