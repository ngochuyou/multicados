/**
 * 
 */
package multicados.internal.domain;

import static java.util.Map.entry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextBuilder;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.HandledConsumer;
import multicados.internal.helper.Utils.HandledSupplier;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractGraphWalkerFactory extends ContextBuilder.AbstractContextBuilder {

	private static final Logger logger = LoggerFactory.getLogger(AbstractGraphWalkerFactory.class);

	@SuppressWarnings("rawtypes")
	protected final Map<Class, GraphWalker> walkersMap;

	// @formatter:off
	@SuppressWarnings("rawtypes")
	public <W extends GraphWalker<?>> AbstractGraphWalkerFactory(
			ApplicationContext applicationContext,
			Class<W> walkerType,
			DomainResourceContext resourceContext,
			Collection<Map.Entry<Class, Map.Entry<Class, GraphWalker>>> fixedLogics,
			Supplier<GraphWalker> noopSupplier)
			throws Exception {
		this.walkersMap = Utils.declare(scan(walkerType))
					.second(walkerType)
					.third(applicationContext)
					.triInverse()
				.then(this::contribute)
					.second(fixedLogics)
				.then(this::addFixedLogics)
					.second(resourceContext)
					.third(noopSupplier)
				.thenPrepend(this::buildCollection)
				.then(this::filterNoop)
					.second(resourceContext)
				.then(this::sort)
				.then(this::join)
				.then(Collections::unmodifiableMap)
				.get();
	}
	
	@SuppressWarnings("rawtypes")
	public <W extends GraphWalker<?>> AbstractGraphWalkerFactory(
			ApplicationContext applicationContext,
			Class<W> walkerType,
			DomainResourceContext resourceContext,
			HandledSupplier<Collection<Map.Entry<Class, Map.Entry<Class, GraphWalker>>>, Exception> fixedLogicsSupplier,
			Supplier<GraphWalker> noopSupplier)
			throws Exception {
		this(
				applicationContext,
				walkerType,
				resourceContext,
				fixedLogicsSupplier.get(),
				noopSupplier);
	}
	// @formatter:on

	private <W extends GraphWalker<?>> Set<BeanDefinition> scan(Class<W> walkerType) {
		if (logger.isTraceEnabled()) {
			logger.trace("Scanning for {}", walkerType.getSimpleName());
		}

		final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
				false);

		scanner.addIncludeFilter(new AssignableTypeFilter(walkerType));

		return scanner.findCandidateComponents(Settings.BASE_PACKAGE);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <W extends GraphWalker<?>> Map<Class, Map.Entry<Class, GraphWalker>> contribute(
			ApplicationContext applicationContext, Class<W> walkerType, Set<BeanDefinition> beanDefs) throws Exception {
		final Map<Class, Map.Entry<Class, GraphWalker>> walkersMap = new HashMap<>(8);

		for (final BeanDefinition beanDef : beanDefs) {
			final Class<GraphWalker> walkerClass = (Class<GraphWalker>) Class.forName(beanDef.getBeanClassName());

			if (FixedLogic.class.isAssignableFrom(walkerClass)) {
				continue;
			}

			final For anno = walkerClass.getDeclaredAnnotation(For.class);

			if (anno == null) {
				throw new IllegalArgumentException(For.Message.getMissingMessage(walkerClass));
			}

			final GraphWalker walker = constructWalker(applicationContext, walkerClass);

			walkersMap.put(anno.value(), entry(anno.value(), walker));

			if (logger.isDebugEnabled()) {
				logger.debug("Contributing {}", walker.getLoggableName());
			}
		}

		return walkersMap;
	}

	@SuppressWarnings("rawtypes")
	private GraphWalker constructWalker(ApplicationContext applicationContext, Class<GraphWalker> walkerClass)
			throws Exception {
		// @formatter:off
		return Utils.declare(walkerClass)
				.then(this::locateConstructor)
					.prepend(applicationContext)
				.then(this::construct)
				.get();
		// @formatter:on
	}

	@SuppressWarnings("rawtypes")
	private GraphWalker construct(ApplicationContext applicationContext, Constructor constructor)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return GraphWalker.class.cast(constructor
				.newInstance(Stream.of(constructor.getParameterTypes()).map(applicationContext::getBean).toArray()));
	}

	@SuppressWarnings("rawtypes")
	private Constructor locateConstructor(Class<GraphWalker> walkerClass) {
		Constructor<?>[] constructors = walkerClass.getConstructors();

		if (constructors.length == 1) {
			return constructors[0];
		}

		for (Constructor<?> constructor : constructors) {
			if (constructor.isAnnotationPresent(Autowired.class)) {
				return constructor;
			}
		}

		throw new IllegalArgumentException(String.format("Unable to locate any non-arg nor @%s construct in %s type %s",
				Autowired.class.getSimpleName(), GraphWalker.class.getSimpleName(), walkerClass));
	}

	@SuppressWarnings("rawtypes")
	private Map<Class, LinkedHashSet<Map.Entry<Class, GraphWalker>>> addFixedLogics(
			Map<Class, Map.Entry<Class, GraphWalker>> contributions,
			Collection<Map.Entry<Class, Map.Entry<Class, GraphWalker>>> fixedLogics) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Adding fixed logics");
		}

		final Map<Class, LinkedHashSet<Map.Entry<Class, GraphWalker>>> finalContributions = new HashMap<>();
		final HandledConsumer<Collection<Entry<Class, Map.Entry<Class, GraphWalker>>>, Exception> finalContributionBuilder = (
				walkerEntries) -> {
			for (final Entry<Class, Map.Entry<Class, GraphWalker>> walkerEntry : walkerEntries) {
				final Class resourceType = walkerEntry.getKey();
				final Map.Entry<Class, GraphWalker> walker = walkerEntry.getValue();

				if (finalContributions.containsKey(resourceType)) {
					finalContributions.get(resourceType).add(walker);
					continue;
				}
				// @formatter:off
				Utils.declare(new LinkedHashSet<Map.Entry<Class, GraphWalker>>())
					.consume(walkers -> walkers.add(walker))
					.prepend(resourceType)
					.consume(finalContributions::put);
				// @formatter:on
			}
		};

		finalContributionBuilder.accept(fixedLogics);
		finalContributionBuilder.accept(contributions.entrySet());

		return finalContributions;
	}

	@SuppressWarnings("rawtypes")
	private Map<Class, LinkedHashSet<Map.Entry<Class, GraphWalker>>> buildCollection(
			Map<Class, LinkedHashSet<Map.Entry<Class, GraphWalker>>> mappedWalkers,
			DomainResourceContext resourceContext, Supplier<GraphWalker> noopSupplier) {
		if (logger.isTraceEnabled()) {
			logger.trace("Building collections");
		}

		final Map<Class, LinkedHashSet<Map.Entry<Class, GraphWalker>>> walkersCollections = new HashMap<>();

		for (final DomainResourceGraph graph : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toGraphsList())) {
			final Class resourceType = graph.getResourceType();

			if (!mappedWalkers.containsKey(resourceType)) {
				if (!walkersCollections.containsKey(resourceType)) {
					buildWithoutExsitingCollection(graph, walkersCollections, null, noopSupplier);
					continue;
				}

				buildWithExsitingCollection(graph, walkersCollections, null, noopSupplier);
				continue;
			}

			final LinkedHashSet<Map.Entry<Class, GraphWalker>> scopedWalkers = mappedWalkers.get(resourceType);

			for (final Map.Entry<Class, GraphWalker> walker : scopedWalkers) {
				if (!walkersCollections.containsKey(resourceType)) {
					buildWithoutExsitingCollection(graph, walkersCollections, walker, noopSupplier);
					continue;
				}

				buildWithExsitingCollection(graph, walkersCollections, walker, noopSupplier);
			}
		}

		return walkersCollections;
	}

	@SuppressWarnings("rawtypes")
	private Map<Class, LinkedHashSet<Map.Entry<Class, GraphWalker>>> filterNoop(
			Map<Class, LinkedHashSet<Map.Entry<Class, GraphWalker>>> builtWalkers,
			DomainResourceContext resourceContext, Supplier<GraphWalker> noopSupplier) {
		// @formatter:off
		return builtWalkers.entrySet()
			.stream()
			.map(entry -> Map.entry(
					entry.getKey(),
					entry.getValue().size() > 1
						? entry.getValue()
							.stream()
							.filter(walker -> !walker.getValue().equals(noopSupplier.get()))
							.<LinkedHashSet<Map.Entry<Class, GraphWalker>>>collect(LinkedHashSet::new, (set, walker) -> set.add(walker), LinkedHashSet::addAll)
						: entry.getValue()))
			.collect(CollectionHelper.toMap());
		// @formatter:on
	}

	@SuppressWarnings("rawtypes")
	private Map<Class, LinkedHashSet<GraphWalker>> sort(
			Map<Class, LinkedHashSet<Map.Entry<Class, GraphWalker>>> builtWalkers,
			DomainResourceContext resourceContext) {
		DomainResourceGraph<DomainResource> graph = resourceContext.getResourceGraph();
		Map<Class, Integer> depthMap = new HashMap<>();
		// @formatter:off
		return builtWalkers.entrySet().stream()
				.map(entry -> entry(
						entry.getKey(),
						entry.getValue().stream()
							.sorted((one, two) -> Integer.compare(locateDepth(one, graph, depthMap), locateDepth(two, graph, depthMap)))
							.map(walkerEntry -> walkerEntry.getValue())
							.<LinkedHashSet<GraphWalker>>collect(LinkedHashSet::new, (set, walker) -> set.add(walker), LinkedHashSet::addAll)))
				.collect(CollectionHelper.toMap());
		// @formatter:on
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private int locateDepth(Map.Entry<Class, GraphWalker> walkerEntry, DomainResourceGraph<DomainResource> graph,
			Map<Class, Integer> depthMap) {
		Class walkerClass = walkerEntry.getKey();

		if (depthMap.containsKey(walkerClass)) {
			return depthMap.get(walkerClass);
		}

		int newDepth = graph.locate(walkerClass).getDepth();

		depthMap.put(walkerClass, newDepth);

		return newDepth;
	}

	@SuppressWarnings("rawtypes")
	private void buildWithExsitingCollection(DomainResourceGraph graph,
			Map<Class, LinkedHashSet<Map.Entry<Class, GraphWalker>>> walkersCollections,
			Map.Entry<Class, GraphWalker> contribution, Supplier<GraphWalker> noopSupplier) {
		final Class resourceType = graph.getResourceType();
		final LinkedHashSet<Map.Entry<Class, GraphWalker>> exsitingWalkersCollection = walkersCollections
				.get(resourceType);

		if (graph.getParent() == null) {
			if (contribution == null) {
				return;
			}

			walkersCollections.put(resourceType, addAll(exsitingWalkersCollection, from(contribution)));
			return;
		}

		final LinkedHashSet<Map.Entry<Class, GraphWalker>> parentWalkersCollection = walkersCollections
				.get(graph.getParent().getResourceType());

		if (contribution == null) {
			walkersCollections.put(resourceType, addAll(parentWalkersCollection, exsitingWalkersCollection));
			return;
		}

		walkersCollections.put(resourceType,
				addAll(parentWalkersCollection, addAll(exsitingWalkersCollection, from(contribution))));
		return;
	}

	@SuppressWarnings("rawtypes")
	private void buildWithoutExsitingCollection(DomainResourceGraph graph,
			Map<Class, LinkedHashSet<Map.Entry<Class, GraphWalker>>> graphCollections,
			Map.Entry<Class, GraphWalker> contribution, Supplier<GraphWalker> noopSupplier) {
		final Class resourceType = graph.getResourceType();

		if (graph.getParent() == null) {
			if (contribution == null) {
				graphCollections.put(resourceType, noop(noopSupplier));
				return;
			}

			graphCollections.put(resourceType, from(contribution));
			return;
		}

		final LinkedHashSet<Map.Entry<Class, GraphWalker>> parentWalkersCollection = graphCollections
				.get(graph.getParent().getResourceType());

		if (contribution == null) {
			graphCollections.put(resourceType, parentWalkersCollection);
			return;
		}

		graphCollections.put(resourceType, addAll(parentWalkersCollection, from(contribution)));
		return;
	}

	@SuppressWarnings("rawtypes")
	private LinkedHashSet<Map.Entry<Class, GraphWalker>> noop(Supplier<GraphWalker> noopSupplier) {
		return new LinkedHashSet<>(List.of(Map.entry(DomainResource.class, noopSupplier.get())));
	}

	@SuppressWarnings("rawtypes")
	private LinkedHashSet<Map.Entry<Class, GraphWalker>> from(Map.Entry<Class, GraphWalker> walker) {
		return new LinkedHashSet<>(List.of(walker));
	}

	@SuppressWarnings("rawtypes")
	private LinkedHashSet<Map.Entry<Class, GraphWalker>> addAll(
			LinkedHashSet<Map.Entry<Class, GraphWalker>> walkersCollection,
			LinkedHashSet<Map.Entry<Class, GraphWalker>> entry) {
		final LinkedHashSet<Map.Entry<Class, GraphWalker>> copy = new LinkedHashSet<>(walkersCollection);

		copy.addAll(entry);

		return copy;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Class, GraphWalker> join(Map<Class, LinkedHashSet<GraphWalker>> builtWalkers) {
		// @formatter:off
		return builtWalkers.entrySet()
				.stream()
				.map(entry -> Map.entry(
						entry.getKey(),
						entry.getValue()
							.stream()
							.reduce((product, walker) -> product.and(walker))
							.get()))
				.collect(CollectionHelper.toMap());
		// @formatter:on
	}

	public static interface FixedLogic {
	}

}
