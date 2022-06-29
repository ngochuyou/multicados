/**
 *
 */
package multicados.internal.helper;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.springframework.data.jpa.domain.Specification;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public class SpecificationHelper {

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

}
