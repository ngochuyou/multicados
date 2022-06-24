/**
 * 
 */
package multicados.internal.domain.validation;

import static java.util.Map.entry;
import static multicados.internal.helper.Utils.asIf;
import static multicados.internal.helper.Utils.declare;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import multicados.internal.config.Settings;
import multicados.internal.domain.AbstractGraphLogicsFactory;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraphCollectors;
import multicados.internal.domain.Exclude;
import multicados.internal.domain.GraphLogic;
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
public class DomainResourceValidatorFactoryImpl extends AbstractGraphLogicsFactory
		implements DomainResourceValidatorFactory {

	public static final Logger logger = LoggerFactory.getLogger(DomainResourceValidatorFactoryImpl.class);

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
					final int minLength = SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_MIN_LENGTH, val -> Integer.valueOf(val), 1);
					final String fieldName = SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_DEFAULT_FIELD_NAME, HandledFunction.identity(), NAMED_RESOURCE_DEFAULT_FIELD_NAME);
					final String literal = declare(SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_ACCEPTED_LITERAL, HandledFunction.identity(), NAMED_RESOURCE_DEFAULT_LITERAL_KEY_FOR_VIETNAMESE))
							.then(configuredLiteralKey ->
									asIf(AVAILABLE_LITERALS.containsKey(configuredLiteralKey))
									.then(() -> AVAILABLE_LITERALS.get(configuredLiteralKey))
									.<String>elseThrowAndReturn(() -> new IllegalArgumentException(String.format("Unknown literal key %s", configuredLiteralKey))))
							.get();
					final NamedResourceValidator defaultNamedResourceValidator = new NamedResourceValidator(fieldName, minLength, maxLength, literal, acceptedCharacters, isNaturalAlphabeticAccepted, isNaturalNumericAccepted);
					final List<Map.Entry<Class, Map.Entry<Class, GraphLogic>>> fixedLogics = new ArrayList<>();

					for (final Class<DomainResource> resourceType: resourceContextProvider.getResourceGraph().collect(DomainResourceGraphCollectors.toTypesSet())) {
						if (!NamedResource.class.isAssignableFrom(resourceType)) {
							continue;
						}
						
						if (!resourceType.isAnnotationPresent(Exclude.class) ||
								!resourceType.getDeclaredAnnotation(Exclude.class)
									.value().equals(NamedResource.class)) {
							fixedLogics.add(entry(resourceType, entry(NamedResource.class, defaultNamedResourceValidator)));
							continue;
						}
					}
					
					return fixedLogics;
				},
				() -> NO_OP_VALIDATOR);
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends DomainResource> DomainResourceValidator<T> getValidator(Class<T> resourceType) {
		return (DomainResourceValidator<T>) logicsMap.get(resourceType);
	}

	@Override
	public void summary() {
		for (@SuppressWarnings("rawtypes")
		final Entry<Class, GraphLogic> entry : logicsMap.entrySet()) {
			logger.debug("Using {} for {}", entry.getValue().getLoggableName(), entry.getKey().getSimpleName());
		}
	}

	@SuppressWarnings("rawtypes")
	private static final DomainResourceValidator NO_OP_VALIDATOR = new AbstractDomainResourceValidator<DomainResource>() {

		@Override
		public Validation isSatisfiedBy(DomainResource resource) {
			return Validation.success();
		}

		@Override
		public Validation isSatisfiedBy(Serializable id, DomainResource resource) {
			return Validation.success();
		}

		@SuppressWarnings("unchecked")
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
			implements AbstractGraphLogicsFactory.FixedLogic {

		private final String fieldName;
		private final String errorMessage;
		private final Pattern pattern;

		// @formatter:off
		private NamedResourceValidator(
				String fieldName,
				int minLength,
				int maxLength,
				String literal,
				List<Character> acceptedCharacters,
				boolean acceptNaturalAlphabet,
				boolean acceptNaturalNumeric) throws Exception {
			if (logger.isTraceEnabled()) {
				logger.trace("Instantiating {}", NamedResourceValidator.class.getName());
			}
			
			pattern = Pattern.compile(declare(RegexHelper.start())
					.then(RegexBuilder::group)
					.then(self -> StringHelper.hasLength(literal) ? self.literal(literal) : self)
					.then(self -> acceptNaturalAlphabet ? self.naturalAlphabet() : self)
					.then(self -> acceptNaturalNumeric ? self.naturalNumeric() : self)
					.then(self -> CollectionHelper.isEmpty(acceptedCharacters) ? self : self.literal(acceptedCharacters))
					.then(RegexGroupBuilder::end)
					.then(RegexBuilder::withLength)
					.then(self -> self.min(minLength).max(maxLength))
					.then(RegexBuilder::end)
					.then(RegexBuilder::build)
					.get());
			this.fieldName = fieldName;
			this.errorMessage = declare(acceptedCharacters)
					.consume(characters -> asIf(acceptNaturalAlphabet).then(f -> characters.add('L')).orElse(f -> {}))
					.consume(characters -> asIf(acceptNaturalNumeric).then(f -> characters.add('N')).orElse(f -> {}))
					.then(characters -> characters.stream().map(Common::name).collect(Collectors.joining(StringHelper.COMMON_JOINER)))
					.then(Common::invalidPattern)
						.second(Common.invalidLength(minLength, maxLength))
					.then((one, two) -> List.of(one, two))
					.then((messages) -> StringHelper.join(StringHelper.SPACE, messages))
					.get();
			
			if (logger.isDebugEnabled()) {
				logger.debug("Compliant pattern is {} for field [{}]", pattern, fieldName);
			}
		}
		// @formatter:on

		@Override
		public Validation isSatisfiedBy(NamedResource resource) {
			return isSatisfiedBy(null, resource);
		}

		@Override
		public Validation isSatisfiedBy(Serializable id, NamedResource resource) {
			Validation result = Validation.success();

			if (resource.getName() == null || !pattern.matcher(resource.getName()).matches()) {
				result.bad(fieldName, errorMessage);
			}

			return result;
		}

	};

}
