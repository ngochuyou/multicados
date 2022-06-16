/**
 * 
 */
package multicados.internal.helper;

import static java.util.Map.entry;
import static multicados.internal.helper.StringHelper.SPACE;
import static multicados.internal.helper.StringHelper.join;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * @author Ngoc Huy
 *
 */
public abstract class Common {

	private Common() {}

	private static final String COMMON_TEMPLATE = "%s %s";

	private static final String NOT_EMPTY = "must not be empty";

	private static final String USER = "User";
	private static final String FILE = "File";
	private static final String RESOURCE = "Resource";

	private static final String ERROR = "error";
	private static final String MESSAGE = "message";
	private static final String PAYLOAD = "payload";

	private static final String GENERAL_OK_MESSAGE = "Successfully done";
	private static final String UNKNOWN_ERROR = "Unknown error";
	private static final String NOT_FOUND_TEMPLATE = "%s not found";

	public static String notEmpty(String... prefix) {
		return String.format(COMMON_TEMPLATE, StringHelper.join(List.of(prefix)), NOT_EMPTY);
	}

	public static Map<String, String> error(String error) {
		return Map.of(ERROR, StringHelper.hasLength(error) ? error : UNKNOWN_ERROR);
	}

	public static <T> Map<String, Object> payload(T payload) {
		return Map.of(PAYLOAD, payload, MESSAGE, GENERAL_OK_MESSAGE);
	}

	public static <T> Map<String, Object> payload(T payload, String message) {
		return Map.of(PAYLOAD, payload, MESSAGE, message);
	}

	public static String file(String filename) {
		return join(SPACE, List.of(FILE, filename));
	}

	public static String user(String username) {
		return join(SPACE, List.of(USER, username));
	}

	public static String notFound(Collection<String> preficies) {
		if (CollectionHelper.isEmpty(preficies)) {
			return notFound(List.of(RESOURCE));
		}

		return String.format(NOT_FOUND_TEMPLATE, join(SPACE, preficies));
	}

	// @formatter:off
	static final Map<Character, String> SYMBOL_NAMES = Map.ofEntries(
			entry('|', "vertical bar"),
			entry(';', "semicolon"),
			entry(':', "colon"),
			entry('.', "period"),
			entry('(', "opening parenthesis"),
			entry(')', "closing parenthesis"),
			entry('\s', "space"),
			entry(',', "comma"),
			entry('-', "hyphen"),
			entry('_', "underscore"),
			entry('"', "quote"),
			entry('\'', "apostrophe"),
			entry('/', "slash"),
			entry('\\', "back slash"),
			entry('!', "exclamation"),
			entry('@', "at sign"),
			entry('#', "numero sign"),
			entry('$', "dollar sign"),
			entry('%', "percent sign"),
			entry('&', "ampersand"),
			entry('*', "asterisk"),
			entry('?', "question mark"));
	// @formatter:on

}
