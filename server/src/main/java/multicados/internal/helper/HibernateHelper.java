/**
 *
 */
package multicados.internal.helper;

import java.io.Serializable;

import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.data.jpa.domain.Specification;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public class HibernateHelper {

	@SuppressWarnings("rawtypes")
	private static final Specification EMPTY = (root, query, builder) -> builder.conjunction();

	@SuppressWarnings("unchecked")
	public static <T> Specification<T> none() {
		return EMPTY;
	}

	public static <D extends DomainResource> Specification<D> hasId(Class<D> type, Serializable id,
			SharedSessionContract session) {
		return (root, query,
				builder) -> builder.equal(root.get(((SharedSessionContractImplementor) session).getFactory()
						.unwrap(SessionFactoryImplementor.class).getMetamodel().entityPersister(type)
						.getEntityMetamodel().getIdentifierProperty().getName()), id);
	}

	public static SessionFactoryImplementor unwrapToSfi(SessionFactory sessionFactory) {
		return sessionFactory.unwrap(SessionFactoryImplementor.class);
	}
	
	@SuppressWarnings("unchecked")
	public static <D extends DomainResource> D loadProxyInternal(Class<D> type, Serializable id,
			SharedSessionContract session) {
		if (!SharedSessionContractImplementor.class.isAssignableFrom(session.getClass())) {
			throw new IllegalArgumentException(
					String.format("session must be of type %s", SharedSessionContractImplementor.class));
		}

		final SharedSessionContractImplementor sessionContract = (SharedSessionContractImplementor) session;
		final EntityPersister persister = sessionContract.getFactory().getMetamodel().entityPersister(type);

		return (D) sessionContract.internalLoad(persister.getEntityName(), id, false, false);
	}

}
