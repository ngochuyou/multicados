/**
 * 
 */
package multicados.internal.helper;

import java.util.Collection;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

import multicados.domain.AbstractEntity;
import multicados.internal.context.ContextManager;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.repository.Selector;

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

	public static <D extends DomainResource, E> Selector<D, E> toSelector(Collection<String> properties) {
		return (root, query, builder) -> properties.stream().map(root::get).collect(Collectors.toList());
	}

}
