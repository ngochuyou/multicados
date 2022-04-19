/**
 * 
 */
package multicados.internal.helper;

import java.io.Serializable;

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

	public static <D extends DomainResource> Specification<D> hasId(Class<D> type, Serializable id) {
		return (root, query, builder) -> builder.equal(root.get(
				HibernateHelper.getEntityPersister(type).getEntityMetamodel().getIdentifierProperty().getName()), id);
	}

}
