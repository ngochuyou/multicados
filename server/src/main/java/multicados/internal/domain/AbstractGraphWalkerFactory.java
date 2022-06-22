/**
 * 
 */
package multicados.internal.domain;

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
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.HandledConsumer;
import multicados.internal.helper.Utils.HandledSupplier;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractGraphWalkerFactory {

	public static interface FixedLogic {
	}

	@SuppressWarnings("rawtypes")
	protected final Map<Class, GraphWalker> walkersMap;

	// @formatter:off
	@SuppressWarnings("rawtypes")
	public <W extends GraphWalker<?>> AbstractGraphWalkerFactory(
			ApplicationContext applicationContext,
			Class<W> walkerType,
			DomainResourceContext resourceContext,
			Collection<Map.Entry<Class, GraphWalker>> fixedLogics,
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
				.then(this::buildCollection)
				.then(this::join)
				.then(Collections::unmodifiableMap)
				.get();
	}
	
	@SuppressWarnings("rawtypes")
	public <W extends GraphWalker<?>> AbstractGraphWalkerFactory(
			ApplicationContext applicationContext,
			Class<W> walkerType,
			DomainResourceContext resourceContext,
			HandledSupplier<Collection<Map.Entry<Class, GraphWalker>>, Exception> fixedLogicsSupplier,
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
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		logger.trace("Scanning for {}", walkerType.getSimpleName());

		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

		scanner.addIncludeFilter(new AssignableTypeFilter(walkerType));

		return scanner.findCandidateComponents(Settings.BASE_PACKAGE);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <W extends GraphWalker<?>> Map<Class, GraphWalker> contribute(ApplicationContext applicationContext,
			Class<W> walkerType, Set<BeanDefinition> beanDefs) throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());
		final Map<Class, GraphWalker> walkersMap = new HashMap<>(8);

		for (BeanDefinition beanDef : beanDefs) {
			Class<GraphWalker> walkerClass = (Class<GraphWalker>) Class.forName(beanDef.getBeanClassName());

			if (FixedLogic.class.isAssignableFrom(walkerClass)) {
				continue;
			}

			For anno = walkerClass.getDeclaredAnnotation(For.class);

			if (anno == null) {
				throw new IllegalArgumentException(For.Message.getMissingMessage(walkerClass));
			}

			GraphWalker walker = constructWalker(applicationContext, walkerClass);

			walkersMap.put(anno.value(), walker);
			logger.trace("Contributing {}", walker.getLoggableName());
		}

		logger.trace("Contributed {} {}", walkersMap.size(), walkerType.getSimpleName());

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
	private Map<Class, LinkedHashSet<GraphWalker>> addFixedLogics(Map<Class, GraphWalker> contributions,
			Collection<Map.Entry<Class, GraphWalker>> fixedLogics) throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		logger.trace("Adding fixed logics");

		Map<Class, LinkedHashSet<GraphWalker>> finalContributions = new HashMap<>();
		HandledConsumer<Collection<Entry<Class, GraphWalker>>, Exception> finalContributionBuilder = (
				walkerEntries) -> {
			for (Entry<Class, GraphWalker> walkerEntry : walkerEntries) {
				final Class resourceType = walkerEntry.getKey();
				final GraphWalker walker = walkerEntry.getValue();

				if (finalContributions.containsKey(resourceType)) {
					finalContributions.get(resourceType).add(walker);
					continue;
				}

				// @formatter:off
				Utils.declare(new LinkedHashSet<GraphWalker>())
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
	private Map<Class, LinkedHashSet<GraphWalker>> buildCollection(Map<Class, LinkedHashSet<GraphWalker>> mappedWalkers,
			DomainResourceContext resourceContext, Supplier<GraphWalker> noopSupplier) {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		logger.trace("Building collections");

		final Map<Class, LinkedHashSet<GraphWalker>> walkersCollections = new HashMap<>();

		for (DomainResourceGraph graph : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toGraphsList())) {
			Class resourceType = graph.getResourceType();

			if (!mappedWalkers.containsKey(resourceType)) {
				if (!walkersCollections.containsKey(resourceType)) {
					buildWithoutExsitingCollection(graph, walkersCollections, null, noopSupplier);
					continue;
				}

				buildWithExsitingCollection(graph, walkersCollections, null, noopSupplier);
				continue;
			}

			LinkedHashSet<GraphWalker> scopedWalkers = mappedWalkers.get(resourceType);

			for (GraphWalker walker : scopedWalkers) {
				if (!walkersCollections.containsKey(resourceType)) {
					buildWithoutExsitingCollection(graph, walkersCollections, walker, noopSupplier);
					continue;
				}

				buildWithExsitingCollection(graph, walkersCollections, walker, noopSupplier);
			}
		}
		// @formatter:off
		return walkersCollections.entrySet()
			.stream()
			.map(entry -> Map.entry(
					entry.getKey(),
					entry.getValue().size() > 1
						? entry.getValue()
							.stream()
							.filter(walker -> !walker.equals(noopSupplier.get()))
							.<LinkedHashSet<GraphWalker>>collect(LinkedHashSet::new, (set, walker) -> set.add(walker), LinkedHashSet::addAll)
						: entry.getValue()))
			.collect(CollectionHelper.toMap());
		// @formatter:on
	}

	@SuppressWarnings("rawtypes")
	private void buildWithExsitingCollection(DomainResourceGraph graph,
			Map<Class, LinkedHashSet<GraphWalker>> walkersCollections, GraphWalker contribution,
			Supplier<GraphWalker> noopSupplier) {
		Class resourceType = graph.getResourceType();
		LinkedHashSet<GraphWalker> exsitingWalkersCollection = walkersCollections.get(resourceType);

		if (graph.getParent() == null) {
			if (contribution == null) {
				return;
			}

			walkersCollections.put(resourceType, addAll(exsitingWalkersCollection, from(contribution)));
			return;
		}

		LinkedHashSet<GraphWalker> parentWalkersCollection = walkersCollections
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
			Map<Class, LinkedHashSet<GraphWalker>> graphCollections, GraphWalker contribution,
			Supplier<GraphWalker> noopSupplier) {
		Class resourceType = graph.getResourceType();

		if (graph.getParent() == null) {
			if (contribution == null) {
				graphCollections.put(resourceType, noop(noopSupplier));
				return;
			}

			graphCollections.put(resourceType, from(contribution));
			return;
		}

		LinkedHashSet<GraphWalker> parentWalkersCollection = graphCollections.get(graph.getParent().getResourceType());

		if (contribution == null) {
			graphCollections.put(resourceType, parentWalkersCollection);
			return;
		}

		graphCollections.put(resourceType, addAll(parentWalkersCollection, from(contribution)));
		return;
	}

	@SuppressWarnings("rawtypes")
	private LinkedHashSet<GraphWalker> noop(Supplier<GraphWalker> noopSupplier) {
		return new LinkedHashSet<>(List.of(noopSupplier.get()));
	}

	@SuppressWarnings("rawtypes")
	private LinkedHashSet<GraphWalker> from(GraphWalker walker) {
		return new LinkedHashSet<>(List.of(walker));
	}

	@SuppressWarnings("rawtypes")
	private LinkedHashSet<GraphWalker> addAll(LinkedHashSet<GraphWalker> walkersCollection,
			LinkedHashSet<GraphWalker> entry) {
		LinkedHashSet<GraphWalker> copy = new LinkedHashSet<>(walkersCollection);

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
						entry.getValue().stream()
							.reduce((product, walker) -> product.and(walker))
							.get()))
				.collect(CollectionHelper.toMap());
		// @formatter:on
	}

}
