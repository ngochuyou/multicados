/**
 * 
 */
package multicados.internal.domain.builder;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Constants;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceTree;
import multicados.internal.domain.DomainResourceTreeCollectors;
import multicados.internal.domain.For;
import multicados.internal.domain.IdentifiableDomainResource;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.PermanentResource;
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

	public DomainResourceBuilderFactoryImpl(DomainResourceContext resourceContext) throws Exception {
		// @formatter:off
		this.buildersMap = Utils.declare(scan())
				.then(this::contribute)
				.then(this::mapFixedLogics)
					.second(resourceContext)
				.then(this::buildCollection)
				.then(this::join)
				.then(Collections::unmodifiableMap)
				.get();
		// @formatter:on
	}

	private Set<BeanDefinition> scan() {
		final Logger logger = LoggerFactory.getLogger(DomainResourceBuilderFactoryImpl.class);

		logger.trace("Scanning for {}", DomainResourceBuilder.class.getSimpleName());

		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

		scanner.addIncludeFilter(new AssignableTypeFilter(DomainResourceBuilder.class));

		return scanner.findCandidateComponents(Constants.BASE_PACKAGE);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<Class, DomainResourceBuilder> contribute(Set<BeanDefinition> beanDefs)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		final Logger logger = LoggerFactory.getLogger(this.getClass());
		final Map<Class, DomainResourceBuilder> buildersMap = new HashMap<>(8);

		for (BeanDefinition beanDef : beanDefs) {
			Class<DomainResourceBuilder> builderClass = (Class<DomainResourceBuilder>) Class
					.forName(beanDef.getBeanClassName());
			For anno = builderClass.getDeclaredAnnotation(For.class);

			if (anno == null) {
				throw new IllegalArgumentException(For.MISSING_MESSAGE);
			}

			DomainResourceBuilder builder = TypeHelper.constructFromNonArgs(builderClass);

			buildersMap.put(anno.value(), builder);
			logger.trace("Contributing {}", builder.getLoggableName());
		}

		logger.trace("Contributed {} builders", buildersMap.size());

		return buildersMap;
	}

	@SuppressWarnings("rawtypes")
	private Map<Class, DomainResourceBuilder> mapFixedLogics(Map<Class, DomainResourceBuilder> contributions) {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		logger.trace("Adding fixed logics");

		contributions.put(IdentifiableDomainResource.class, IDENTIFIABLE_RESOURCE_BUILDER);
		contributions.put(PermanentResource.class, PERMANENT_RESOURCE_BUILDER);
		contributions.put(NamedResource.class, NAMED_RESOURCE_BUILDER);

		return contributions;
	}

	@SuppressWarnings("rawtypes")
	private Map<Class, LinkedHashSet<DomainResourceBuilder>> buildCollection(
			Map<Class, DomainResourceBuilder> mappedBuilders, DomainResourceContext resourceContext) {
		final Logger logger = LoggerFactory.getLogger(this.getClass());
		final Map<Class, LinkedHashSet<DomainResourceBuilder>> buildersCollections = new HashMap<>();

		for (DomainResourceTree tree : resourceContext.getResourceTree()
				.collect(DomainResourceTreeCollectors.toTreesList())) {
			Class resourceType = tree.getResourceType();
			DomainResourceBuilder builder = mappedBuilders.get(resourceType);

			if (!buildersCollections.containsKey(resourceType)) {
				buildWithoutExsitingCollection(tree, buildersCollections, builder);
				continue;
			}

			buildWithExsitingCollection(tree, buildersCollections, builder);
		}

		buildersCollections.entrySet()
				.forEach(pair -> logger.trace("{}: {}", pair.getKey().getName(),
						buildersCollections.get(pair.getKey()).stream().map(entry -> entry.getLoggableName())
								.collect(Collectors.joining(StringHelper.COMMON_JOINER))));

		return buildersCollections;
	}

	@SuppressWarnings("rawtypes")
	private void buildWithExsitingCollection(DomainResourceTree tree,
			Map<Class, LinkedHashSet<DomainResourceBuilder>> buildersCollections, DomainResourceBuilder contribution) {
		Class resourceType = tree.getResourceType();
		LinkedHashSet<DomainResourceBuilder> exsitingBuildersCollection = buildersCollections.get(resourceType);

		if (isNoop(exsitingBuildersCollection)) {
			buildWithoutExsitingCollection(tree, buildersCollections, contribution);
			return;
		}

		if (tree.getParent() == null) {
			if (contribution == null) {
				return;
			}

			buildersCollections.put(resourceType, addAll(exsitingBuildersCollection, from(contribution)));
			return;
		}

		LinkedHashSet<DomainResourceBuilder> parentBuildersCollection = buildersCollections
				.get(tree.getParent().getResourceType());

		if (contribution == null) {
			if (isNoop(parentBuildersCollection)) {
				return;
			}

			buildersCollections.put(resourceType, addAll(parentBuildersCollection, exsitingBuildersCollection));
			return;
		}

		buildersCollections.put(resourceType,
				addAll(parentBuildersCollection, addAll(exsitingBuildersCollection, from(contribution))));
		return;
	}

	@SuppressWarnings("rawtypes")
	private void buildWithoutExsitingCollection(DomainResourceTree tree,
			Map<Class, LinkedHashSet<DomainResourceBuilder>> buildersCollections, DomainResourceBuilder contribution) {
		Class resourceType = tree.getResourceType();

		if (tree.getParent() == null) {
			if (contribution == null) {
				buildersCollections.put(resourceType, noop());
				return;
			}

			buildersCollections.put(resourceType, from(contribution));
			return;
		}

		LinkedHashSet<DomainResourceBuilder> parentBuildersCollection = buildersCollections
				.get(tree.getParent().getResourceType());

		if (contribution == null) {
			buildersCollections.put(resourceType, parentBuildersCollection);
			return;
		}

		if (isNoop(parentBuildersCollection)) {
			buildersCollections.put(resourceType, from(contribution));
			return;
		}

		buildersCollections.put(resourceType, addAll(parentBuildersCollection, from(contribution)));
		return;
	}

	@SuppressWarnings("rawtypes")
	private LinkedHashSet<DomainResourceBuilder> addAll(LinkedHashSet<DomainResourceBuilder> buildersCollection,
			LinkedHashSet<DomainResourceBuilder> entry) {
		LinkedHashSet<DomainResourceBuilder> copy = new LinkedHashSet<>(buildersCollection);

		copy.addAll(entry);

		return copy;
	}

	@SuppressWarnings({ "rawtypes" })
	private boolean isNoop(LinkedHashSet<DomainResourceBuilder> candidate) {
		return candidate.size() == 1 && candidate.contains(NO_OP_BUILDER);
	}

	@SuppressWarnings("rawtypes")
	private LinkedHashSet<DomainResourceBuilder> noop() {
		return new LinkedHashSet<>(List.of(NO_OP_BUILDER));
	}

	@SuppressWarnings("rawtypes")
	private LinkedHashSet<DomainResourceBuilder> from(DomainResourceBuilder builder) {
		return new LinkedHashSet<>(List.of(builder));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Class, DomainResourceBuilder> join(Map<Class, LinkedHashSet<DomainResourceBuilder>> builtBuilders) {
		// @formatter:off
		return builtBuilders.entrySet()
				.stream()
				.map(entry -> Map.entry(
						entry.getKey(),
						entry.getValue().stream()
							.reduce((product, builder) -> product.and(builder))
							.get()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		// @formatter:on
	}

//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	private void mergeBuilders(Map<Class, DomainResourceBuilder> buildersMap,
//			Map.Entry<Class, DomainResourceBuilder> entry) {
//		Class key = entry.getKey();
//		DomainResourceBuilder requestedBuilder = entry.getValue();
//		DomainResourceBuilder exsitingBuilder = buildersMap.get(key);
//
//		if (exsitingBuilder == null) {
//			buildersMap.put(key, requestedBuilder);
//			return;
//		}
//
//		if (exsitingBuilder.contains(requestedBuilder) || requestedBuilder.contains(exsitingBuilder)) {
//			return;
//		}
//
//		buildersMap.put(key,
//				exsitingBuilder == NO_OP_BUILDER ? requestedBuilder : exsitingBuilder.and(requestedBuilder));
//	}
//
//	@SuppressWarnings({ "rawtypes" })
//	private Map<Class, DomainResourceBuilder> chain(Map<Class, DomainResourceBuilder> buildersMap,
//			DomainResourceContext resourceContext) throws Exception {
//		final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//		logger.trace("Chaining builders");
//
//		resourceContext.getResourceTree().forEach(node -> {
//			chainParentLogic(buildersMap, node);
//			chainImplementedLogics(buildersMap, node.getResourceType());
//		});
//
//		return buildersMap;
//	}
//
//	@SuppressWarnings("rawtypes")
//	private Map<Class, DomainResourceBuilder> chainImplementedLogics(Map<Class, DomainResourceBuilder> buildersMap,
//			Class resourceType) {
//		// @formatter:off
//		final Map<Class, DomainResourceBuilder> implementedBuilders = Map.of(
//				IdentifiableDomainResource.class, IDENTIFIABLE_RESOURCE_BUILDER,
//				PermanentResource.class, PERMANENT_RESOURCE_BUILDER,
//				NamedResource.class, NAMED_RESOURCE_BUILDER);
//		// @formatter:on
//		for (Class interfaceType : ClassUtils.getAllInterfacesForClassAsSet(resourceType).stream()
//				.filter(implementedBuilders::containsKey).collect(Collectors.toSet())) {
//			mergeBuilders(buildersMap, Map.entry(resourceType, implementedBuilders.get(interfaceType)));
//		}
//
//		return buildersMap;
//	}
//
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	private Map<Class, DomainResourceBuilder> chainParentLogic(Map<Class, DomainResourceBuilder> buildersMap,
//			DomainResourceTree<? extends DomainResource> node) {
//		Class<DomainResource> resourceType = (Class<DomainResource>) node.getResourceType();
//		DomainResourceBuilder builder = buildersMap.get(resourceType);
//		System.out.println(resourceType.getName());
//		if (builder == null) {
//			if (node.getParent() == null) {
//				buildersMap.put(resourceType, NO_OP_BUILDER);
//				return buildersMap;
//			}
//
//			buildersMap.put(resourceType, locateParentBuilder(buildersMap, node, resourceType));
//			System.out.println("parent: " + buildersMap.get(resourceType).getLoggableName());
//			return buildersMap;
//		}
//
//		if (node.getParent() == null) {
//			return buildersMap;
//		}
//
//		DomainResourceBuilder parentBuilder = locateParentBuilder(buildersMap, node, resourceType);
//
//		if (parentBuilder == builder) {
//			return buildersMap;
//		}
//		System.out.println("child: " + parentBuilder.getLoggableName());
//		System.out.println("parent: " + builder.getLoggableName());
//		buildersMap.put(resourceType, parentBuilder == NO_OP_BUILDER ? builder : parentBuilder.and(builder));
//		System.out.println("==========");
//		return buildersMap;
//	}
//
//	@SuppressWarnings("rawtypes")
//	private DomainResourceBuilder locateParentBuilder(Map<Class, DomainResourceBuilder> builderMap,
//			DomainResourceTree<? extends DomainResource> node, Class<DomainResource> resourceType) {
//		DomainResourceBuilder parentBuilder = builderMap.get(node.getParent().getResourceType());
//
//		Assert.notNull(parentBuilder, String.format("null value found for parent %s of type %s",
//				DomainResourceBuilder.class.getSimpleName(), resourceType.getName()));
//
//		return parentBuilder;
//	}

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

	private static final AbstractDomainResourceBuilder<NamedResource> NAMED_RESOURCE_BUILDER = new AbstractDomainResourceBuilder<>() {

		@Override
		public <E extends NamedResource> E buildInsertion(Serializable id, E resource, EntityManager entityManager)
				throws Exception {
			resource.setName(StringHelper.normalizeString(resource.getName()));
			return resource;
		}

		@Override
		public <E extends NamedResource> E buildUpdate(Serializable id, E model, E resource,
				EntityManager entityManger) {
			resource.setName(StringHelper.normalizeString(model.getName()));
			return resource;
		}

		@Override
		public String getLoggableName() {
			return "NamedResourceBuilder";
		}

		@Override
		public String toString() {
			return getLoggableName();
		}

	};

	private static final AbstractDomainResourceBuilder<PermanentResource> PERMANENT_RESOURCE_BUILDER = new AbstractDomainResourceBuilder<>() {
		@Override
		public <E extends PermanentResource> E buildInsertion(Serializable id, E resource, EntityManager entityManager)
				throws Exception {
			resource.setActive(Boolean.TRUE);
			return resource;
		}

		@Override
		public <E extends PermanentResource> E buildUpdate(Serializable id, E model, E resource,
				EntityManager entityManger) {
			return resource;
		}

		@Override
		public String getLoggableName() {
			return "PermanentResourceBuilder";
		}

		@Override
		public String toString() {
			return getLoggableName();
		}

	};

	@SuppressWarnings("rawtypes")
	private static final AbstractDomainResourceBuilder<IdentifiableDomainResource> IDENTIFIABLE_RESOURCE_BUILDER = new AbstractDomainResourceBuilder<>() {

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
		public <E extends IdentifiableDomainResource> E buildInsertion(Serializable id, E resource,
				EntityManager entityManager) throws Exception {
			if (id == null) {
				return resource;
			}
			// @formatter:off
			id = HANDLER_RESOLVERS
					.get(TYPE_KEY_RESOLVERS.get(id.getClass()))
					.apply(id);
			// @formatter:on
			resource.setId(id);

			return resource;
		}

		@Override
		public <E extends IdentifiableDomainResource> E buildUpdate(Serializable id, E model, E resource,
				EntityManager entityManger) {
			return resource;
		}

		@Override
		public String getLoggableName() {
			return "IdentifiableResourceBuilder";
		}

		@Override
		public String toString() {
			return getLoggableName();
		}

	};

	private static final AbstractDomainResourceBuilder<DomainResource> NO_OP_BUILDER = new AbstractDomainResourceBuilder<DomainResource>() {

		@Override
		public <E extends DomainResource> E buildInsertion(Serializable id, E resource, EntityManager entityManager)
				throws Exception {
			return resource;
		}

		@Override
		public <E extends DomainResource> E buildUpdate(Serializable id, E model, E resource,
				EntityManager entityManger) {
			return resource;
		}

		@Override
		public String getLoggableName() {
			return "<<NO_OP>>";
		}

		@Override
		public String toString() {
			return getLoggableName();
		}

	};

}
