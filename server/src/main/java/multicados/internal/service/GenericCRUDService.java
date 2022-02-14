/**
 * 
 */
package multicados.internal.service;

import java.io.Serializable;

import javax.persistence.EntityManager;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface GenericCRUDService extends Service, ContextBuilder {

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

}
