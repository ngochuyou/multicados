/**
 * 
 */
package multicados.internal.helper;

import java.util.List;

/**
 * @author Ngoc Huy
 *
 */
public class Common {

	private Common() {}

	private static final String COMMON_TEMPLATE = "%s %s";

	private static final String NOT_EMPTY = "must not be empty";

	public static String notEmpty(String... prefix) {
		return String.format(COMMON_TEMPLATE, StringHelper.join(List.of(prefix)), NOT_EMPTY);
	}

}
