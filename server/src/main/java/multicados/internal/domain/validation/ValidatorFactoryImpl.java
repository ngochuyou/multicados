/**
 * 
 */
package multicados.internal.domain.validation;

import static multicados.internal.helper.Utils.declare;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import multicados.internal.config.Constants;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContextProvider;
import multicados.internal.domain.DomainResourceTree;
import multicados.internal.domain.For;

/**
 * @author Ngoc Huy
 *
 */
public class ValidatorFactoryImpl implements ValidatorFactory {

	@SuppressWarnings("rawtypes")
	private final Map<Class<DomainResource>, Validator> validatorMap;

	public ValidatorFactoryImpl(DomainResourceContextProvider resourceContextProvider) throws Exception {
		// @formatter:off
		this.validatorMap = declare(scan())
				.then(this::register)
					.second(resourceContextProvider)
				.then(this::chain)
					.second(resourceContextProvider)
				.then(this::chainFixedValidators)
				.then(Collections::unmodifiableMap)
				.get();
		// @formatter:on
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Set<Class<Validator>> scan() throws ClassNotFoundException {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		scanner.addIncludeFilter(new AssignableTypeFilter(Validator.class));

		logger.trace("Scanning for {}", Validator.class.getSimpleName());

		Set<BeanDefinition> candidates = scanner.findCandidateComponents(Constants.BASE_PACKAGE);

		logger.trace("Found {} candidate(s)", candidates.size());

		Set<Class<Validator>> validatorClasses = new HashSet<>();

		for (BeanDefinition validatorBeanDef : candidates) {
			Class<Validator> validatorType = (Class<Validator>) Class.forName(validatorBeanDef.getBeanClassName());

			if (!validatorType.isAnnotationPresent(For.class)) {
				throw new IllegalArgumentException(String.format("%s on %s of type [%s]", For.MISSING_MESSAGE,
						Validator.class.getSimpleName(), validatorType.getName()));
			}

			validatorClasses.add(validatorType);
		}

		return validatorClasses;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Class<DomainResource>, Validator> register(Set<Class<Validator>> validatorClasses) throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		logger.trace("Registering {}(s)", Validator.class.getSimpleName());

		Map<Class<DomainResource>, Validator> validators = new HashMap<>(0);
		For annotation;

		for (Class<Validator> validatorClass : validatorClasses) {
			try {
				// can not be null here since scan already asserted it
				annotation = validatorClass.getDeclaredAnnotation(For.class);

				validators.put((Class<DomainResource>) annotation.value(),
						validatorClass.getConstructor().newInstance());
			} catch (NoSuchMethodException nsme) {
				throw new NoSuchMethodException(String.format(
						"Unable to locate a %s with no arguments in %s of type [%s]", Constructor.class.getSimpleName(),
						Validator.class.getSimpleName(), validatorClass.getName()));
			}
		}

		return validators;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Class<DomainResource>, Validator> chain(Map<Class<DomainResource>, Validator> validators,
			DomainResourceContextProvider resourceContextProvider) throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		logger.trace("Chaining {}(s)", Validator.class.getSimpleName());
		// @formatter:off
		resourceContextProvider.getResourceTree()
			.forEach(node -> {
				Class<DomainResource> resourceType = (Class<DomainResource>) node.getResourceType();
				Validator validator = validators.get(resourceType);
				
				if (validator == null) {
					if (node.getParent() == null) {
						validators.put(resourceType, NO_OP_VALIDATOR);
						return;
					}

					validators.put(resourceType, locateParentValidator(validators, node, resourceType));
					
					return;
				}
				
				if (node.getParent() == null) {
					return;
				}

				Validator parentValidator = locateParentValidator(validators, node, resourceType);
				
				validators.put(resourceType, parentValidator == NO_OP_VALIDATOR ? validator : parentValidator.and(validator));
			});
		// @formatter:on
		return validators;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Class<DomainResource>, Validator> chainFixedValidators(Map<Class<DomainResource>, Validator> validators,
			DomainResourceContextProvider resourceContextProvider) throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());
		final Map<Class<DomainResource>, Validator> fixedValidators = Map.of();

		logger.trace("Chaining fixed {}(s)", Validator.class.getSimpleName());
		// @formatter:off
		resourceContextProvider.getResourceTree()
			.forEach(node -> {
				Class<DomainResource> resourceType = (Class<DomainResource>) node.getResourceType();
				Validator validator = validators.get(resourceType);
				
				for (Class<?> interfaceType: ClassUtils.getAllInterfacesForClassAsSet(resourceType)) {
					if (fixedValidators.containsKey(interfaceType)) {
						if (validator != NO_OP_VALIDATOR) {
							validators.put(resourceType, fixedValidators.get(interfaceType).and(validator));
							return;
						}
						
						validators.put(resourceType, fixedValidators.get(interfaceType));
					}
				}
			});
		// @formatter:on
		return validators;
	}

	@SuppressWarnings("rawtypes")
	private Validator locateParentValidator(Map<Class<DomainResource>, Validator> validators,
			DomainResourceTree<? extends DomainResource> node, Class<DomainResource> resourceType) {
		Validator parentValidator = validators.get(node.getParent().getResourceType());

		Assert.notNull(parentValidator, String.format("null value found for parent %s of type %s",
				Validator.class.getSimpleName(), resourceType.getName()));

		return parentValidator;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends DomainResource> Validator<T> getValidator(Class<T> resourceType) {
		return (Validator<T>) validatorMap.get(resourceType);
	}

	@Override
	public void summary() throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		validatorMap.entrySet()
				.forEach(entry -> logger.debug("[{}] -> [{}]", entry.getKey().getName(), entry.getValue().getLoggableName()));
	}

	@SuppressWarnings("rawtypes")
	private static final Validator NO_OP_VALIDATOR = new Validator() {

		@Override
		public Validation isSatisfiedBy(DomainResource resource) {
			return Validation.success();
		}

		@Override
		public Validation isSatisfiedBy(Serializable id, DomainResource resource) {
			return Validation.success();
		}

		@Override
		public Validator and(Validator next) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getLoggableName() {
			return "<<NO_OP>>";
		}

	};

}
