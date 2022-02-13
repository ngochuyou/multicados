/**
 * 
 */
package multicados.internal.service;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.hibernate.SharedSessionContract;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.DomainResource;
import multicados.internal.helper.HibernateHelper;

/**
 * @author Ngoc Huy
 *
 */
public interface GenericCRUDService extends Service, ContextBuilder {

	default <E extends DomainResource> ServiceResult create(Serializable id, E model, Class<E> type) {
		return create(id, model, type, false);
	}

	default <E extends DomainResource> ServiceResult create(Serializable id, E model, Class<E> type,
			boolean flushOnFinish) {
		return create(id, model, type, HibernateHelper.getCurrentSession(), flushOnFinish);
	}

	<E extends DomainResource> ServiceResult create(Serializable id, E model, Class<E> type,
			EntityManager entityManager, boolean flushOnFinish);

}
