/**
 * 
 */
package multicados.internal.domain.builder;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.SharedSessionContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import multicados.internal.config.Constants;
import multicados.internal.context.ContextManager;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContextProvider;
import multicados.internal.domain.DomainResourceTree;
import multicados.internal.domain.Entity;
import multicados.internal.domain.For;
import multicados.internal.domain.PermanentResource;
import multicados.internal.domain.repository.GenericRepositoryImpl;
import multicados.internal.helper.FunctionHelper.HandledFunction;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceBuilderFactoryImpl implements DomainResourceBuilderFactory {

	@SuppressWarnings("rawtypes")
	private final Map<Class, DomainResourceBuilder> buildersMap;

	public DomainResourceBuilderFactoryImpl() throws Exception {
		// @formatter:off
		this.buildersMap = Utils.declare(scan())
				.then(this::constructBuilders)
				.then(this::chain)
				.then(this::addFixedBuilders)
				.then(Collections::unmodifiableMap)
				.get();
		// @formatter:on
	}

	private Set<BeanDefinition> scan() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		final Logger logger = LoggerFactory.getLogger(DomainResourceBuilderFactoryImpl.class);

		scanner.addIncludeFilter(new AssignableTypeFilter(DomainResourceBuilder.class));
		// @formatter:off
		Stream.of(this.getClass().getDeclaredClasses())
			.filter(DomainResourceBuilder.class::isAssignableFrom)
			.map(AssignableTypeFilter::new)
			.forEach(scanner::addExcludeFilter);
		// @formatter:on
		logger.trace("Scanning for {}", DomainResourceBuilder.class.getSimpleName());

		return scanner.findCandidateComponents(Constants.BASE_PACKAGE);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<Class, DomainResourceBuilder> constructBuilders(Set<BeanDefinition> beanDefs)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		final Logger logger = LoggerFactory.getLogger(GenericRepositoryImpl.class);

		logger.trace("Constructing builders");

		Map<Class, DomainResourceBuilder> buildersMap = new HashMap<>(8);

		for (BeanDefinition beanDef : beanDefs) {
			Class<DomainResourceBuilder> builderClass = (Class<DomainResourceBuilder>) Class
					.forName(beanDef.getBeanClassName());
			For anno = builderClass.getDeclaredAnnotation(For.class);

			if (anno == null) {
				throw new IllegalArgumentException(For.MISSING_MESSAGE);
			}

			buildersMap.put(anno.value(), TypeHelper.constructFromNonArgs(builderClass));
		}

		return buildersMap;
	}

	@SuppressWarnings("rawtypes")
	private Map<Class, DomainResourceBuilder> addFixedBuilders(Map<Class, DomainResourceBuilder> buildersMap)
			throws Exception {
		final Logger logger = LoggerFactory.getLogger(GenericRepositoryImpl.class);

		logger.trace("Adding fixed builders");
		// @formatter:off
		return Utils.declare(buildersMap)
				.then(this::addFromInheritance)
				.then(this::addFromImplements)
				.get();
		// @formatter:on
	}

	@SuppressWarnings("rawtypes")
	private Map<Class, DomainResourceBuilder> addFromImplements(Map<Class, DomainResourceBuilder> buildersMap) {
		final Map<Class, DomainResourceBuilder> fixedBuilders = Map.of(PermanentResource.class,
				PERMANENT_RESOURCE_BUILDER);

		for (Class type : buildersMap.keySet()) {
			for (Class interfaceType : ClassUtils.getAllInterfacesForClassAsSet(type).stream()
					.filter(fixedBuilders::containsKey).collect(Collectors.toSet())) {
				mergeBuilders(buildersMap, Map.entry(type, fixedBuilders.get(interfaceType)));
			}
		}

		return buildersMap;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Class, DomainResourceBuilder> addFromInheritance(Map<Class, DomainResourceBuilder> buildersMap) {
		final Map<Class, DomainResourceBuilder> fixedBuilders = Map.of(Entity.class, ENTITY_BUILDER);
		// @formatter:off
		for (Class type : buildersMap.keySet()) {
			Stack<Class> inheritance = TypeHelper.getClassStack(type);
			
			for (Class parentType: inheritance.stream().filter(fixedBuilders::containsKey).collect(Collectors.toSet())) {
				mergeBuilders(buildersMap, Map.entry(type, fixedBuilders.get(parentType)));
			}
		}
		// @formatter:on
		return buildersMap;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void mergeBuilders(Map<Class, DomainResourceBuilder> buildersMap,
			Map.Entry<Class, DomainResourceBuilder> entry) {
		Class key = entry.getKey();
		DomainResourceBuilder requestedBuilder = entry.getValue();
		DomainResourceBuilder exsitingBuilder = buildersMap.get(key);

		if (exsitingBuilder == null) {
			buildersMap.put(key, requestedBuilder);
			return;
		}

		if (requestedBuilder == exsitingBuilder) {
			return;
		}

		buildersMap.put(key,
				exsitingBuilder == NO_OP_BUILDER ? requestedBuilder : requestedBuilder.and(exsitingBuilder));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Class, DomainResourceBuilder> chain(Map<Class, DomainResourceBuilder> buildersMap) throws Exception {
		final Logger logger = LoggerFactory.getLogger(GenericRepositoryImpl.class);

		logger.trace("Chaining builders");

		final DomainResourceContextProvider resourceContext = ContextManager
				.getBean(DomainResourceContextProvider.class);

		resourceContext.getResourceTree().forEach(node -> {
			Class<DomainResource> resourceType = (Class<DomainResource>) node.getResourceType();
			DomainResourceBuilder builder = buildersMap.get(resourceType);

			if (builder == null) {
				if (node.getParent() == null) {
					buildersMap.put(resourceType, NO_OP_BUILDER);
					return;
				}

				buildersMap.put(resourceType, locateParentBuilder(buildersMap, node, resourceType));
				return;
			}

			if (node.getParent() == null) {
				return;
			}

			DomainResourceBuilder parentBuilder = locateParentBuilder(buildersMap, node, resourceType);

			if (parentBuilder == builder) {
				return;
			}

			buildersMap.put(resourceType, parentBuilder == NO_OP_BUILDER ? builder : parentBuilder.and(builder));
		});

		return buildersMap;
	}

	@SuppressWarnings("rawtypes")
	private DomainResourceBuilder locateParentBuilder(Map<Class, DomainResourceBuilder> builderMap,
			DomainResourceTree<? extends DomainResource> node, Class<DomainResource> resourceType) {
		DomainResourceBuilder parentBuilder = builderMap.get(node.getParent().getResourceType());

		Assert.notNull(parentBuilder, String.format("null value found for parent %s of type %s",
				DomainResourceBuilder.class.getSimpleName(), resourceType.getName()));

		return parentBuilder;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends DomainResource, T extends DomainResourceBuilder<E>> DomainResourceBuilder<E> getBuilder(
			Class<E> resourceClass) {
		return buildersMap.get(resourceClass);
	}

	@Override
	public void summary() throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		buildersMap.entrySet().forEach(
				entry -> logger.debug("[{}] -> [{}]", entry.getKey().getName(), entry.getValue().getLoggableName()));
	}

	private static final AbstractDomainResourceBuilder<PermanentResource> PERMANENT_RESOURCE_BUILDER = new AbstractDomainResourceBuilder<>() {
		@Override
		public <E extends PermanentResource> E buildInsertion(Serializable id, E resource,
				SharedSessionContract session) throws Exception {
			resource.setActive(Boolean.TRUE);
			return resource;
		}

		@Override
		public <E extends PermanentResource> E buildUpdate(Serializable id, E model, E resource,
				SharedSessionContract session) {
			return resource;
		}

		@Override
		public String getLoggableName() {
			return "PermanentResourceBuilder";
		}

	};

	@SuppressWarnings("rawtypes")
	private static final AbstractDomainResourceBuilder<Entity> ENTITY_BUILDER = new AbstractDomainResourceBuilder<>() {

		private static final Map<Class<? extends Serializable>, Class<? extends Serializable>> TYPE_KEY_RESOLVERS = Map
				.of(String.class, String.class);

		private static final Map<Class<? extends Serializable>, HandledFunction<Serializable, Serializable, Exception>> HANDLER_RESOLVERS;

		static {
			Map<Class<? extends Serializable>, HandledFunction<Serializable, Serializable, Exception>> handlerResolvers = new HashMap<>(
					8);

			handlerResolvers.put(String.class, id -> StringHelper.normalizeString((String) id));
			handlerResolvers.put(null, id -> id);

			HANDLER_RESOLVERS = Collections.unmodifiableMap(handlerResolvers);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <E extends Entity> E buildInsertion(Serializable id, E resource, SharedSessionContract session)
				throws Exception {
			// @formatter:off
			id = HANDLER_RESOLVERS
					.get(TYPE_KEY_RESOLVERS.get(id.getClass()))
					.apply(id);
			// @formatter:on
			resource.setId(id);

			return resource;
		}

		@Override
		public <E extends Entity> E buildUpdate(Serializable id, E model, E resource, SharedSessionContract session) {
			return resource;
		}

		@Override
		public String getLoggableName() {
			return "EntityBuilder";
		}

	};

	private static final AbstractDomainResourceBuilder<DomainResource> NO_OP_BUILDER = new AbstractDomainResourceBuilder<DomainResource>() {

		@Override
		public <E extends DomainResource> E buildInsertion(Serializable id, E resource, SharedSessionContract session)
				throws Exception {
			return resource;
		}

		@Override
		public <E extends DomainResource> E buildUpdate(Serializable id, E model, E resource,
				SharedSessionContract session) {
			return resource;
		}

		@Override
		public String getLoggableName() {
			return "<<NO_OP>>";
		}

	};

}
