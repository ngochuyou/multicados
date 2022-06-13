/**
 * 
 */
package multicados.internal.helper;

import java.io.Serializable;

import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.spi.SessionImplementor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public class SpecificationHelper {

	private static final String UNABLE_TO_LOCATE_IDENTIFIER_TEMPLATE = String.format(
			"Unable to locate identifier for entity type [%s], requires a %s instance of type %s", "%s",
			Session.class.getSimpleName(), SessionImplementor.class.getSimpleName());

	@SuppressWarnings("rawtypes")
	private static final Specification EMPTY = (root, query, builder) -> builder.conjunction();

	@SuppressWarnings("unchecked")
	public static <T> Specification<T> none() {
		return EMPTY;
	}

	public static <D extends DomainResource> Specification<D> hasId(Class<D> type, Serializable id,
			SharedSessionContract session) {
		Assert.isTrue(SessionImplementor.class.isAssignableFrom(session.getClass()),
				String.format(UNABLE_TO_LOCATE_IDENTIFIER_TEMPLATE, type.getName()));

		return (root, query,
				builder) -> builder.equal(root.get(SessionImplementor.class.cast(session).getSessionFactory()
						.getMetamodel().entityPersister(type).getEntityMetamodel().getIdentifierProperty().getName()),
						id);
	}

}
