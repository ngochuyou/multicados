/**
 * 
 */
package nh.multicados.internal.helper;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import multicados.internal.helper.RegexHelper;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
public class RegexHelperTests {

	@Test
	public void testRegexBuilder() {
		// @formatter:off
		String regex = RegexHelper
				.start()
					.group()
						.literal(StringHelper.VIETNAMESE_CHARACTERS)
						.naturalAlphabet()
						.naturalNumeric()
						.literal(List.of(
								'\s', '.', ',', '(', ')',
								'[', ']', '_', '-',
								'!', '@', '#', '$',
								'&', '*', '\'', '\\',
								'"', '?'))
					.end()
					.withLength().min(0).max(255)
				.end()
				.build();
		// @formatter:on
		assertTrue(!"#Trần@<script></script>".matches(regex));
		assertTrue("![#Trần Vũ Ngọc-Huy?]@,(*)&_.$''\"".matches(regex));
	}
}
