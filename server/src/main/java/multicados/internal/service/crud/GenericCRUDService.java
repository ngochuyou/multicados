/**
 * 
 */
package multicados.internal.service.crud;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Pageable;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.DomainResource;
import multicados.internal.service.Service;
import multicados.internal.service.ServiceResult;
import multicados.internal.service.crud.security.CRUDCredential;

/**
 * @author Ngoc Huy
 *
 */
public interface GenericCRUDService<TUPLE> extends Service, ContextBuilder {

	default <E extends DomainResource> ServiceResult create(Serializable id, E model, Class<E> type,
			EntityManager entityManager) {
		return create(id, model, type, entityManager, false);
	}

	<E extends DomainResource> ServiceResult create(Serializable id, E model, Class<E> type,
			EntityManager entityManager, boolean flushOnFinish);

	default <E extends DomainResource> ServiceResult update(Serializable id, E model, Class<E> type,
			EntityManager entityManager) {
		return update(id, model, type, entityManager, false);
	}

	<E extends DomainResource> ServiceResult update(Serializable id, E model, Class<E> type,
			EntityManager entityManager, boolean flushOnFinish);

	<E extends DomainResource> List<TUPLE> readAll(Class<E> type, Collection<String> properties, Pageable pageable,
			CRUDCredential credential, EntityManager entityManager);

	<E extends DomainResource> List<TUPLE> readAll(Class<E> type, Collection<String> properties,
			CRUDCredential credential, EntityManager entityManager);

}
