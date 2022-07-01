/**
 *
 */
package multicados.internal.domain.validation;

import static java.util.Map.entry;
import static multicados.internal.helper.Utils.declare;
import static multicados.internal.helper.Utils.doWhen;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import multicados.internal.config.Settings;
import multicados.internal.context.DomainLogicUtils;
import multicados.internal.context.Loggable;
import multicados.internal.domain.AbstractGraphLogicsFactory;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraphCollectors;
import multicados.internal.domain.GraphLogic;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.annotation.Name;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.Common;
import multicados.internal.helper.RegexHelper;
import multicados.internal.helper.RegexHelper.RegexBuilder;
import multicados.internal.helper.RegexHelper.RegexGroupBuilder;
import multicados.internal.helper.SpringHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.HandledFunction;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceValidatorFactoryImpl extends AbstractGraphLogicsFactory
		implements DomainResourceValidatorFactory {

	private static final Logger logger = LoggerFactory.getLogger(DomainResourceValidatorFactoryImpl.class);

	private static final String NAMED_RESOURCE_DEFAULT_LITERAL_KEY_FOR_VIETNAMESE = "vi";
	private static final Map<String, String> AVAILABLE_LITERALS = Map
			.of(NAMED_RESOURCE_DEFAULT_LITERAL_KEY_FOR_VIETNAMESE, StringHelper.VIETNAMESE_CHARACTERS);

	private static final Map<String, Character> TRANSLATED_CONFIGURATION_CHARACTERS = Map.of("<space>", '\s');

	private static volatile boolean isFixedLogicsBuilt = false;

	@Autowired
	public DomainResourceValidatorFactoryImpl(Environment env, ApplicationContext applicationContext,
			DomainResourceContext resourceContextProvider) throws Exception {
		// @formatter:off
		super(
				applicationContext,
				DomainResourceValidator.class,
				resourceContextProvider,
				() -> NO_OP_VALIDATOR);
		// @formatter:on
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected Collection<Entry<Class, Entry<Class, GraphLogic>>> getFixedLogics(ApplicationContext applicationContext)
			throws Exception {
		if (isFixedLogicsBuilt) {
			throw new IllegalAccessException(Utils.Access.getClosedMessage(new Loggable() {}));
		}

		final List<Entry<Class, Entry<Class, GraphLogic>>> fixedLogics = new ArrayList<>();

		fixedLogics
				.addAll((Collection<? extends Entry<Class, Entry<Class, GraphLogic>>>) constructNamedResourceValidators(
						applicationContext));

		if (logger.isTraceEnabled()) {
			logger.trace(Utils.Access.getClosingMessage(new Loggable() {}));
		}

		return fixedLogics;
		// @formatter:on
	}

	private Collection<Entry<Class<? extends NamedResource>, Entry<Class<? extends NamedResource>, GraphLogic<? extends NamedResource>>>> constructNamedResourceValidators(
			ApplicationContext applicationContext) throws Exception {
		final Environment env = applicationContext.getBean(Environment.class);
		// @formatter:off
		final List<Character> acceptedCharacters = Stream.of(SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_ACCEPTED_CHARS, HandledFunction.identity(), StringHelper.EMPTY_STRING)
				.split(StringHelper.WHITESPACE_CHAR_CLASS))
				.map(val -> DomainResourceValidatorFactoryImpl.TRANSLATED_CONFIGURATION_CHARACTERS.containsKey(val) ? DomainResourceValidatorFactoryImpl.TRANSLATED_CONFIGURATION_CHARACTERS.get(val) : val.charAt(0) )
				.collect(ArrayList::new, (list, entry) -> list.add(entry), ArrayList::addAll);
		final boolean isNaturalAlphabeticAccepted = SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_ACCEPTED_NATURAL_ALPHABET, val -> Boolean.valueOf(val), true);
		final boolean isNaturalNumericAccepted = SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_ACCEPTED_NATURAL_NUMERIC, val -> Boolean.valueOf(val), true);
		final int maxLength = SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_MAX_LENGTH, val -> Integer.valueOf(val), 255);
		final int minLength = SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_MIN_LENGTH, val -> Integer.valueOf(val), 1);
		final String literal = declare(SpringHelper.getOrDefault(env, Settings.DOMAIN_NAMED_RESOURCE_ACCEPTED_LITERAL, HandledFunction.identity(), DomainResourceValidatorFactoryImpl.NAMED_RESOURCE_DEFAULT_LITERAL_KEY_FOR_VIETNAMESE))
				.then(configuredLiteralKey ->
						Utils.<String>when(DomainResourceValidatorFactoryImpl.AVAILABLE_LITERALS.containsKey(configuredLiteralKey))
							.then(() -> DomainResourceValidatorFactoryImpl.AVAILABLE_LITERALS.get(configuredLiteralKey))
							.orElseThrow(() -> new IllegalArgumentException(String.format("Unknown literal key %s", configuredLiteralKey))))
				.get();
		final Pattern pattern = declare(RegexHelper.start())
				.then(RegexBuilder::group)
					.then(self -> StringHelper.hasLength(literal) ? self.literal(literal) : self)
					.then(self -> isNaturalAlphabeticAccepted ? self.naturalAlphabet() : self)
					.then(self -> isNaturalNumericAccepted ? self.naturalNumeric() : self)
					.then(self -> CollectionHelper.isEmpty(acceptedCharacters) ? self
							: self.literal(acceptedCharacters))
				.then(RegexGroupBuilder::end)
					.then(RegexBuilder::withLength)
					.then(self -> self.min(minLength).max(maxLength))
				.then(RegexBuilder::end)
				.then(RegexBuilder::build)
				.then(Pattern::compile)
				.get();
		final String errorMessage = declare(acceptedCharacters).consume(
				characters -> doWhen(isNaturalAlphabeticAccepted).then(f -> characters.add('L')).orElse(f -> {}))
				.consume(characters -> doWhen(isNaturalNumericAccepted).then(f -> characters.add('N')).orElse(f -> {}))
				.then(characters -> characters.stream().map(Common::name)
						.collect(Collectors.joining(StringHelper.COMMON_JOINER)))
				.then(Common::invalidPattern).second(Common.invalidLength(minLength, maxLength))
				.then((one, two) -> List.of(one, two))
				.then((messages) -> StringHelper.join(StringHelper.SPACE, messages)).get();
		// @formatter:on
		final DomainLogicUtils logicUtils = applicationContext.getBean(DomainLogicUtils.class);
		final List<Entry<Class<? extends NamedResource>, Entry<Class<? extends NamedResource>, GraphLogic<? extends NamedResource>>>> validators = new ArrayList<>();
		final GenericRepository genericRepository = applicationContext.getBean(GenericRepository.class);

		for (final Class<? extends NamedResource> resourceType : getNamedResourceTypes(
				applicationContext.getBean(DomainResourceContext.class))) {
			final Field scopedField = logicUtils.getScopingMetadata(resourceType).getScopedAttributeNames().get(0);
			// @Name is guaranteed to present here, see
			// DomainLogicUtils#getMetadataProvider()
			if (!scopedField.getDeclaredAnnotation(Name.class).useDefault()) {
				continue;
			}

			validators.add(entry(resourceType, entry(resourceType,
					new NamedResourceValidator(genericRepository, scopedField.getName(), pattern, errorMessage))));
		}

		return validators;
	}

	@SuppressWarnings("unchecked")
	private List<Class<? extends NamedResource>> getNamedResourceTypes(DomainResourceContext resourceContext) {
		final List<Class<? extends NamedResource>> resourceTypes = new ArrayList<>();

		for (final Class<? extends DomainResource> resourceType : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toTypesSet())) {
			if (!NamedResource.class.isAssignableFrom(resourceType)
					|| Modifier.isInterface(resourceType.getModifiers())) {
				continue;
			}

			resourceTypes.add((Class<? extends NamedResource>) resourceType);
		}

		return resourceTypes;
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
		public Validation isSatisfiedBy(EntityManager entityManager, DomainResource resource) {
			return isSatisfiedBy(entityManager, null, resource);
		}

		@Override
		public Validation isSatisfiedBy(EntityManager entityManager, Serializable id, DomainResource resource) {
			return isSatisfiedBy(null, null, resource);
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

}
