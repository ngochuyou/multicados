/**
 * 
 */
package multicados.internal.helper;

import static java.util.Map.entry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.StringUtils;

/**
 * @author Ngoc Huy
 *
 */
public class StringHelper extends StringUtils {

	public static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final String PATH_JOINNER = "\\";
	public static final String COMMON_JOINER = ", ";

	public static final String EMPTY_STRING = "";
	public static final String SPACE = " ";
	public static final String DOT = ".";
	public static final String COMMA = ",";
	public static final String NULL = "null";
	public static final String VIETNAMESE_CHARACTERS = "ÁáÀàẢảÃãẠạĂăẮắẰằẲẳẴẵẶặÂâẤấẦầẨẩẪẫẬậĐđÉéÈèẺẻẼẽẸẹÊêỂểẾếỀềỄễỆệÍíÌìỊịỈỉĨĩỊịÓóÒòỎỏÕõỌọÔôỐốỒồỔổỖỗỘộƠơỚớỜờỞởỠỡỢợÚùÙùỦủŨũỤụƯưỨứỪừỬửỮữỰựÝýỲỳỶỷỸỹỴỵ";

	// @formatter:off
	private static final Map<Character, String> SYMBOL_NAMES = Map.ofEntries(
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
	public static final String symbolNamesOf(Character... characters) {
		return Stream.of(characters).map(SYMBOL_NAMES::get).filter(Objects::nonNull)
				.collect(Collectors.joining(COMMON_JOINER));
	}

	private static final MessageDigest SHA_256_MD;

	static {
		MessageDigest digest;
		SecureRandom random = new SecureRandom();

		try {
			digest = MessageDigest.getInstance("SHA-256");

			byte[] salt = new byte[16];

			random.nextBytes(salt);
			digest.update(salt);
		} catch (NoSuchAlgorithmException nsae) {
			digest = null;
			nsae.printStackTrace();
			System.exit(-1);
		}

		SHA_256_MD = digest;
	}

	public static String path(Collection<String> pathNodes) {
		return join(PATH_JOINNER, pathNodes);
	}

	public static <T> String join(Collection<T> elements) {
		return join((value) -> Optional.ofNullable(value).map(Object::toString).orElse(NULL), elements);
	}

	public static <T> String join(Function<T, String> stringGetter, Collection<T> elements) {
		return join(COMMON_JOINER, stringGetter, elements);
	}

	public static <T> String join(CharSequence joiner, Collection<String> elements) {
		return elements.stream().collect(Collectors.joining(joiner));
	}

	public static <T> String join(CharSequence joiner, Function<T, String> stringGetter, Collection<T> elements) {
		return elements.stream().map(stringGetter).collect(Collectors.joining(joiner));
	}

	public static final String WHITESPACE_CHARS = "\\u0009" // CHARACTER TABULATION
			+ "\\u000A" // LINE FEED (LF)
			+ "\\u000B" // LINE TABULATION
			+ "\\u000C" // FORM FEED (FF)
			+ "\\u000D" // CARRIAGE RETURN (CR)
			+ "\\u0020" // SPACE
			+ "\\u0085" // NEXT LINE (NEL)4
			+ "\\u00A0" // NO-BREAK SPACE
			+ "\\u1680" // OGHAM SPACE MARK
			+ "\\u180E" // MONGOLIAN VOWEL SEPARATOR
			+ "\\u2000" // EN QUAD
			+ "\\u2001" // EM QUAD
			+ "\\u2002" // EN SPACE
			+ "\\u2003" // EM SPACE
			+ "\\u2004" // THREE-PER-EM SPACE
			+ "\\u2005" // FOUR-PER-EM SPACE
			+ "\\u2006" // SIX-PER-EM SPACE
			+ "\\u2007" // FIGURE SPACE
			+ "\\u2008" // PUNCTUATION SPACE
			+ "\\u2009" // THIN SPACE
			+ "\\u200A" // HAIR SPACE
			+ "\\u2028" // LINE SEPARATOR
			+ "\\u2029" // PARAGRAPH SEPARATOR
			+ "\\u202F" // NARROW NO-BREAK SPACE
			+ "\\u205F" // MEDIUM MATHEMATICAL SPACE
			+ "\\u3000"; // IDEOGRAPHIC SPACE

	public static final String WHITESPACE_CHAR_CLASS = "[" + WHITESPACE_CHARS + "]";

	public static String normalizeString(String string) {
		return hasLength(string) ? string.trim().replaceAll(WHITESPACE_CHAR_CLASS + "+", "\s") : string;
	}

	public static String combineIntoCamel(String first, String second) {
		return toCamel(String.format("%s%s%s", first, SPACE, second), SPACE);
	}

	public static String toCamel(String s, CharSequence seperator) {
		String input = s.trim();

		if (seperator != null) {
			String[] parts = input.split(seperator.toString());

			if (parts.length > 1) {
				StringBuilder builder = new StringBuilder(
						(EMPTY_STRING + parts[0].charAt(0)).toLowerCase() + parts[0].substring(1, parts[0].length()));

				for (int i = 1; i < parts.length; i++) {
					builder.append((EMPTY_STRING + parts[i].charAt(0)).toUpperCase()
							+ parts[i].substring(1, parts[i].length()));
				}

				return builder.toString();
			}
		}

		return (EMPTY_STRING + input.charAt(0)).toLowerCase() + input.substring(1);
	}

	public static String hash(String input) {
		byte[] hashed = SHA_256_MD.digest(input.getBytes(StandardCharsets.UTF_8));

		StringBuilder sb = new StringBuilder();

		for (byte b : hashed)
			sb.append(String.format("%02x", b));

		return sb.toString();
	}

	private static final String NUMERIC_PATTERN = "^[0-9]+$";

	public static final boolean isNumeric(String string) {
		return string.matches(NUMERIC_PATTERN);
	}

}
