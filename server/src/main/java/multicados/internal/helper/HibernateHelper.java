/**
 * 
 */
package multicados.internal.helper;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

import multicados.domain.AbstractEntity;
import multicados.internal.context.ContextManager;

/**
 * @author Ngoc Huy
 *
 */
public class HibernateHelper {

	private HibernateHelper() {}

	public static SessionFactoryImplementor getSessionFactory() {
		return ContextManager.getBean(SessionFactoryImplementor.class);
	}

	public static Session getCurrentSession() {
		return getSessionFactory().getCurrentSession();
	}

	@SuppressWarnings("rawtypes")
	public static <T extends AbstractEntity> EntityPersister getEntityPersister(Class<T> type) {
		return getSessionFactory().getMetamodel().entityPersister(type);
	}

}
