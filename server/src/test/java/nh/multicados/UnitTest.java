/**
 *
 */
package nh.multicados;

import static multicados.internal.helper.StringHelper.VIETNAMESE_CHARACTERS;

import java.util.List;
import java.util.regex.Pattern;

import multicados.internal.helper.RegexHelper;

/**
 * @author Ngoc Huy
 *
 */
public class UnitTest {

	public static void main(String[] args) {
		// @formatter:off
		final List<Character> acceptedDescriptionCharacters = List.of(
				'\s', '.', ',', '(', ')',
				'[', ']', '_', '-', '+',
				'=', '/', '!', '@',
				'#', '$', '%', '^', '&',
				'*', '\'', '"', '?', ':');
		
		System.out.println(Pattern.compile(RegexHelper
				.start()
					.group()
						.literal(VIETNAMESE_CHARACTERS)
						.naturalAlphabet()
						.naturalNumeric()
						.literal(acceptedDescriptionCharacters)
					.end()
					.withLength().atLeastOne().max(255)
				.end()
			.build()).matcher("var random = Math.floor (Math.random() * 10) + 1 The Java @String class replace() method returns a string replacing all the old char or CharSequence to new char or #CharSequence. Since JDK 1.5, a new replace() ...").matches());;
		// @formatter:on
	}
}
