/**
 * 
 */
package multicados.internal.helper;

import org.springframework.data.jpa.domain.Specification;

import multicados.internal.domain.NamedResource;

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

	public static <T extends NamedResource> Specification<T> hasName(T namedResource) {
		return (root, query, builder) -> builder.equal(root.get("name"), namedResource.getName());
	}

}
