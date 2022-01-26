/**
 * 
 */
package multicados.internal.helper;

import org.springframework.data.jpa.domain.Specification;

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

}
