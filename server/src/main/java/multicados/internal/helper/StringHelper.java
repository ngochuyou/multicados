/**
 * 
 */
package multicados.internal.helper;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.StringUtils;

/**
 * @author Ngoc Huy
 *
 */
public class StringHelper extends StringUtils {

	public static final String COMMON_JOINER = ", ";
	public static final String EMPTY_STRING = "";
	public static final String SPACE = " ";
	public static final String VIETNAMESE_CHARACTERS = "ÁáÀàẢảÃãẠạĂăẮắẰằẲẳẴẵẶặÂâẤấẦầẨẩẪẫẬậĐđÉéÈèẺẻẼẽẸẹÊêỂểẾếỀềỄễỆệÍíÌìỊịỈỉĨĩỊịÓóÒòỎỏÕõỌọÔôỐốỒồỔổỖỗỘộƠơỚớỜờỞởỠỡỢợÚùÙùỦủŨũỤụƯưỨứỪừỬửỮữỰựÝýỲỳỶỷỸỹỴỵ";

	public static String join(CharSequence joinner, Object... elements) {
		return Stream.of(elements).map(Object::toString).collect(Collectors.joining(COMMON_JOINER));
	}

	public static String join(Object... elements) {
		return join(COMMON_JOINER, elements);
	}

	public static <T> String join(Function<T, String> stringGetter, T[] elements) {
		return Stream.of(elements).map(stringGetter::apply).collect(Collectors.joining(COMMON_JOINER));
	}

	public static final String WHITESPACE_CHARS = EMPTY_STRING + "\\u0009" // CHARACTER TABULATION
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

}
