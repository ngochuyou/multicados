/**
 * 
 */
package multicados.domain.validator;

import static multicados.internal.helper.StringHelper.VIETNAMESE_CHARACTERS;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import multicados.domain.entity.entities.Category;
import multicados.domain.entity.entities.Category_;
import multicados.internal.domain.For;
import multicados.internal.domain.validation.AbstractDomainResourceValidator;
import multicados.internal.domain.validation.Validation;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.Common;
import multicados.internal.helper.RegexHelper;

/**
 * @author Ngoc Huy
 *
 */
@For(Category.class)
public class CategoryValidator extends AbstractDomainResourceValidator<Category> {

	public static final int MAX_CODE_LENGTH = 5;
	public static final int MAX_DESCRIPTION_LENGTH = 255;
	// @formatter:off
	private static final List<Character> ACCEPTED_DESCRIPTION_CHARACTERS = List.of(
			'\s', '.', ',', '(', ')',
			'[', ']', '_', '-', '+',
			'=', '/', '!', '@',
			'#', '$', '%', '^', '&',
			'*', '\'', '"', '?', ':');
	private static final List<Character> ACCEPTED_DESCRIPTION_CHARACTERS_WITH_IN_MESSAGE =
			CollectionHelper.join(
					Collectors.toList(),
					ACCEPTED_DESCRIPTION_CHARACTERS,
					List.of('N', 'L'));
	
	private static final Pattern DESCRIPTION_PATTERN = Pattern.compile(RegexHelper
			.start()
				.group()
					.literal(VIETNAMESE_CHARACTERS)
					.naturalAlphabet()
					.naturalNumeric()
					.literal(ACCEPTED_DESCRIPTION_CHARACTERS)
				.end()
				.withLength().max(MAX_DESCRIPTION_LENGTH)
			.end()
		.build());
	// @formatter:on
	@Override
	public Validation isSatisfiedBy(Category resource) {
		return isSatisfiedBy(resource.getId(), resource);
	}

	@Override
	public Validation isSatisfiedBy(Serializable id, Category resource) {
		Validation result = Validation.success();

		if (!DESCRIPTION_PATTERN.matcher(resource.getDescription()).matches()) {
			result.bad(Category_.DESCRIPTION, Common.invalidPattern(ACCEPTED_DESCRIPTION_CHARACTERS_WITH_IN_MESSAGE));
		}

		return result;
	}

}
