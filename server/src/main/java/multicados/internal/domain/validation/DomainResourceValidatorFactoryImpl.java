/**
 * 
 */
package multicados.internal.domain.validation;

import static multicados.internal.helper.Utils.asIf;
import static multicados.internal.helper.Utils.declare;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import multicados.internal.config.Settings;
import multicados.internal.domain.AbstractGraphWalkerFactory;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraphCollectors;
import multicados.internal.domain.Exclude;
import multicados.internal.domain.GraphWalker;
import multicados.internal.domain.NamedResource;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.Common;
import multicados.internal.helper.RegexHelper;
import multicados.internal.helper.RegexHelper.RegexBuilder;
import multicados.internal.helper.RegexHelper.RegexGroupBuilder;
import multicados.internal.helper.SpringHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils.HandledFunction;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceValidatorFactoryImpl extends AbstractGraphWalkerFactory
		implements DomainResourceValidatorFactory {

	private static final String NAMED_RESOURCE_DEFAULT_LITERAL_KEY_FOR_VIETNAMESE = "vi";
	private static final String NAMED_RESOURCE_DEFAULT_FIELD_NAME = "name";
	private static final Map<String, String> AVAILABLE_LITERALS = Map
			.of(NAMED_RESOURCE_DEFAULT_LITERAL_KEY_FOR_VIETNAMESE, StringHelper.VIETNAMESE_CHARACTERS);

	private static final Map<String, Character> TRANSLATED_CONFIGURATION_CHARACTERS = Map.of("<space>", '\s');

	@SuppressWarnings("rawtypes")
	@Autowired
	public DomainResourceValidatorFactoryImpl(Environment env, ApplicationContext applicationContext,
			DomainResourceContext resourceContextProvider) throws Exception {
		// @formatter:off
		super(
				applicationContext,
				DomainResourceValidator.class,
				resourceContextProvider,
				() -> {
					final List<Character> acceptedCharacters = Stream.of(SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_ACCEPTED_CHARS, HandledFunction.identity(), StringHelper.EMPTY_STRING)
							.split(StringHelper.WHITESPACE_CHAR_CLASS))
							.map(val -> TRANSLATED_CONFIGURATION_CHARACTERS.containsKey(val) ? TRANSLATED_CONFIGURATION_CHARACTERS.get(val) : val.charAt(0) )
							.collect(ArrayList::new, (list, entry) -> list.add(entry), ArrayList::addAll);
					final boolean isNaturalAlphabeticAccepted = SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_ACCEPTED_NATURAL_ALPHABET, val -> Boolean.valueOf(val), true);
					final boolean isNaturalNumericAccepted = SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_ACCEPTED_NATURAL_NUMERIC, val -> Boolean.valueOf(val), true);
					final int maxLength = SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_MAX_LENGTH, val -> Integer.valueOf(val), 255);
					final String fieldName = SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_DEFAULT_FIELD_NAME, HandledFunction.identity(), NAMED_RESOURCE_DEFAULT_FIELD_NAME);
					final String literal = declare(SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_ACCEPTED_LITERAL, HandledFunction.identity(), NAMED_RESOURCE_DEFAULT_LITERAL_KEY_FOR_VIETNAMESE))
							.then(configuredLiteralKey ->
									asIf(AVAILABLE_LITERALS.containsKey(configuredLiteralKey))
									.then(() -> AVAILABLE_LITERALS.get(configuredLiteralKey))
									.<String>elseThrowAndReturn(() -> new IllegalArgumentException(String.format("Unknown literal key %s", configuredLiteralKey))))
							.get();
					final NamedResourceValidator defaultNamedResourceValidator = new NamedResourceValidator(fieldName, maxLength, literal, acceptedCharacters, isNaturalAlphabeticAccepted, isNaturalNumericAccepted);
					final List<Map.Entry<Class, GraphWalker>> fixedLogics = new ArrayList<>();

					for (Class<DomainResource> resourceType: resourceContextProvider.getResourceGraph().collect(DomainResourceGraphCollectors.toTypesSet())) {
						if (!NamedResource.class.isAssignableFrom(resourceType)) {
							continue;
						}
						
						if (!resourceType.isAnnotationPresent(Exclude.class) ||
								!resourceType.getDeclaredAnnotation(Exclude.class)
									.value().equals(NamedResource.class)) {
							fixedLogics.add(Map.entry(resourceType, defaultNamedResourceValidator));
							continue;
						}
					}
					
					return fixedLogics;
				},
				() -> NO_OP_VALIDATOR);
		// @formatter:on
	}

	public DomainResourceValidator<NamedResource> getDefault() {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends DomainResource> DomainResourceValidator<T> getValidator(Class<T> resourceType) {
		return (DomainResourceValidator<T>) walkersMap.get(resourceType);
	}

	@Override
	public void summary() throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		walkersMap.entrySet().forEach(
				entry -> logger.debug("{} -> {}", entry.getKey().getName(), entry.getValue().getLoggableName()));
	}

	@SuppressWarnings("rawtypes")
	private static final DomainResourceValidator NO_OP_VALIDATOR = new DomainResourceValidator() {

		@Override
		public Validation isSatisfiedBy(DomainResource resource) {
			return Validation.success();
		}

		@Override
		public Validation isSatisfiedBy(Serializable id, DomainResource resource) {
			return Validation.success();
		}

		@Override
		public DomainResourceValidator and(DomainResourceValidator next) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getLoggableName() {
			return "<<NO_OP>>";
		}

	};

	private static class NamedResourceValidator extends AbstractDomainResourceValidator<NamedResource>
			implements AbstractGraphWalkerFactory.FixedLogic {

		private final String fieldName;
		private final String acceptedMessage;
		private final Pattern pattern;

		// @formatter:off
		private NamedResourceValidator(
				String fieldName,
				int maxLength,
				String literal,
				List<Character> acceptedCharacters,
				boolean acceptNaturalAlphabet,
				boolean acceptNaturalNumeric) throws Exception {
			pattern = Pattern.compile(declare(RegexHelper.start())
					.then(RegexBuilder::group)
					.then(self -> StringHelper.hasLength(literal) ? self.literal(literal) : self)
					.then(self -> acceptNaturalAlphabet ? self.naturalAlphabet() : self)
					.then(self -> acceptNaturalNumeric ? self.naturalNumeric() : self)
					.then(self -> CollectionHelper.isEmpty(acceptedCharacters) ? self : self.literal(acceptedCharacters))
					.then(RegexGroupBuilder::end)
					.then(RegexBuilder::withLength)
					.then(self -> self.max(maxLength))
					.then(RegexBuilder::end)
					.then(RegexBuilder::build)
					.get());
			this.fieldName = fieldName;
			this.acceptedMessage = declare(acceptedCharacters)
					.consume(characters -> asIf(acceptNaturalAlphabet).then(f -> characters.add('L')).orElse(f -> {}))
					.consume(characters -> asIf(acceptNaturalNumeric).then(f -> characters.add('N')).orElse(f -> {}))
					.then(characters -> characters.stream().map(Common::name).collect(Collectors.joining(StringHelper.COMMON_JOINER)))
					.get();
		}
		// @formatter:on

		@Override
		public Validation isSatisfiedBy(NamedResource resource) {
			return isSatisfiedBy(null, resource);
		}

		@Override
		public Validation isSatisfiedBy(Serializable id, NamedResource resource) {
			Validation result = Validation.success();

			if (!pattern.matcher(resource.getName()).matches()) {
				result.bad(fieldName, Common.invalidPattern(acceptedMessage));
			}

			return result;
		}

	};

}
