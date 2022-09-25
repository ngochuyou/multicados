/**
 * 
 */
package multicados.controller.mapping;

/**
 * @author Ngoc Huy
 *
 */
public abstract class Mapping {

	private static final String DEPARTMENT = "/department";

	public static interface Endpoint {

		String DEPARTMENT = Mapping.DEPARTMENT;

	}

	public static interface Matcher {

	}

}
