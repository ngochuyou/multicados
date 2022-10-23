/**
 *
 */
package multicados.domain.validator;

import static multicados.internal.helper.StringHelper.VIETNAMESE_CHARACTERS;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import multicados.domain.entity.entities.Category;
import multicados.domain.entity.entities.Category_;
import multicados.internal.domain.annotation.For;
import multicados.internal.domain.validation.AbstractDomainResourceValidator;
import multicados.internal.domain.validation.Validation;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.Common;
import multicados.internal.helper.RegexHelper;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
@For(Category.class)
public class CategoryValidator extends AbstractDomainResourceValidator<Category> {

	public static final int MAX_CODE_LENGTH = 5;
	public static final int MAX_DESCRIPTION_LENGTH = 255;
	// @formatter:off
	private static final String DESCRIPTION_ERROR_MESSAGE;
	private static final Pattern DESCRIPTION_PATTERN;

	static {
		final List<Character> acceptedDescriptionCharacters = List.of(
				'\s', '.', ',', '(', ')',
				'[', ']', '_', '-', '+',
				'=', '/', '!', '@',
				'#', '$', '%', '^', '&',
				'*', '\'', '"', '?', ':');

		DESCRIPTION_PATTERN = Pattern.compile(RegexHelper
				.start()
					.group()
						.literal(VIETNAMESE_CHARACTERS)
						.naturalAlphabet()
						.naturalNumeric()
						.literal(acceptedDescriptionCharacters)
					.end()
					.withLength().atLeastOne().max(MAX_DESCRIPTION_LENGTH)
				.end()
			.build());

		DESCRIPTION_ERROR_MESSAGE =
				StringHelper.join(StringHelper.SPACE, List.of(
						Common.invalidPattern(CollectionHelper.join(
								Collectors.toList(),
								acceptedDescriptionCharacters,
								List.of('N', 'L'))),
						Common.invalidLength(1, MAX_DESCRIPTION_LENGTH)));
	}
	// @formatter:on
	@Override
	public Validation isSatisfiedBy(EntityManager entityManager, Category resource) {
		return isSatisfiedBy(entityManager, null, resource);
	}

	@Override
	public Validation isSatisfiedBy(EntityManager entityManager, Serializable id, Category resource) {
		final Validation result = Validation.success();

		if (resource.getDescription() == null || !DESCRIPTION_PATTERN.matcher(resource.getDescription()).matches()) {
			result.bad(Category_.DESCRIPTION, DESCRIPTION_ERROR_MESSAGE);
		}

		return result;
	}

}
