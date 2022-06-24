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
import java.util.stream.Collectors;

/**
 * @author Ngoc Huy
 *
 */
public abstract class Common {

	private Common() {}

	private static final String COMMON_TEMPLATE = "%s %s";
	private static final String INVALID_PATTERN_TEMPLATE = "Invalid pattern, expect following characters: %s.";
	private static final String NOT_FOUND_TEMPLATE = "%s not found";
	private static final String INVALID_LENGTH_TEMPLATE = "Invalid length, expect length to be in %d and %d range.";

	private static final String NOT_EMPTY = "must not be empty";

	private static final String USER = "User";
	private static final String FILE = "File";
	private static final String RESOURCE = "Resource";

	private static final String ERROR = "error";
	private static final String MESSAGE = "message";
	private static final String PAYLOAD = "payload";

	private static final String GENERAL_OK_MESSAGE = "Successfully done";
	private static final String UNKNOWN_ERROR = "Unknown error";

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

	public static String invalidLength(int min, int max) {
		return String.format(INVALID_LENGTH_TEMPLATE, min, max);
	}
	
	public static String invalidPattern(Collection<Character> characters) {
		return invalidPattern(
				characters.stream().map(Common::name).collect(Collectors.joining(StringHelper.COMMON_JOINER)));
	}

	public static String invalidPattern(String acceptedMessage) {
		return String.format(INVALID_PATTERN_TEMPLATE, acceptedMessage);
	}

	// @formatter:off
	private static final Map<Character, String> SYMBOL_NAMES = Map.ofEntries(
			entry('L', "alphabetical characters"),
			entry('N', "numeric characters"),
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
			entry('?', "question mark"),
			entry('[', "opening hard brackets"),
			entry(']', "closing hard brackets"),
			entry('+', "plus sign"),
			entry('=', "equal sign"),
			entry('^', "caret"));
	private static final Map<String, Character> SYMBOLS = CollectionHelper.inverse(SYMBOL_NAMES);
	// @formatter:on
	public static Character symbol(String symbolName) {
		if (!SYMBOLS.containsKey(symbolName)) {
			throw new IllegalArgumentException(String.format("Unknown name %s", symbolName));
		}

		return SYMBOLS.get(symbolName);
	}

	public static String name(Character symbol) {
		if (!SYMBOL_NAMES.containsKey(symbol)) {
			throw new IllegalArgumentException(String.format("Unknown symbol %s", symbol));
		}

		return SYMBOL_NAMES.get(symbol);
	}

}
