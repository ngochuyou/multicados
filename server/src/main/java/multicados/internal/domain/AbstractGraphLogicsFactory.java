/**
 *
 */
package multicados.internal.domain;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractGraphLogicsFactory extends ContextBuilder.AbstractContextBuilder {

	private static final Logger logger = LoggerFactory.getLogger(AbstractGraphLogicsFactory.class);

	@SuppressWarnings("rawtypes")
	protected final Map<Class, GraphLogic> logicsMap;

	// @formatter:off
	@SuppressWarnings("rawtypes")
	public <W extends GraphLogic<?>> AbstractGraphLogicsFactory(
			ApplicationContext applicationContext,
			Class<W> logicType,
			DomainResourceContext resourceContext,
			Supplier<GraphLogic> noopSupplier)
			throws Exception {
		this.logicsMap = Utils.declare(scan(logicType))
					.second(logicType)
					.third(applicationContext)
					.triInverse()
				.then(this::addContributedLogics)
					.prepend(applicationContext)
				.then(this::addFixedLogics)
					.second(resourceContext)
					.third(noopSupplier)
				.thenPrepend(this::formInheritance)
				.then(this::removeNoopLogics)
					.second(resourceContext)
				.then(this::join)
				.then(Collections::unmodifiableMap)
				.get();
	}
	// @formatter:on
	@SuppressWarnings("rawtypes")
	protected abstract Collection<Entry<Class, Entry<Class, GraphLogic>>> getFixedLogics(
			ApplicationContext applicationContext) throws Exception;

	private <W extends GraphLogic<?>> Set<BeanDefinition> scan(Class<W> logicType) {
		if (logger.isTraceEnabled()) {
			logger.trace("Scanning for {}", logicType.getSimpleName());
		}

		final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
				false);

		scanner.addIncludeFilter(new AssignableTypeFilter(logicType));

		return scanner.findCandidateComponents(Settings.BASE_PACKAGE);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <W extends GraphLogic<?>> Map<Class, LogicEntry> addContributedLogics(ApplicationContext applicationContext,
			Class<W> logicType, Set<BeanDefinition> beanDefs) throws Exception {
		final Map<Class, LogicEntry> logicsMap = new HashMap<>(8);

		for (final BeanDefinition beanDef : beanDefs) {
			final Class<GraphLogic> logicClass = (Class<GraphLogic>) Class.forName(beanDef.getBeanClassName());

			if (FixedLogic.class.isAssignableFrom(logicClass)) {
				continue;
			}

			final For anno = logicClass.getDeclaredAnnotation(For.class);

			if (anno == null) {
				throw new IllegalArgumentException(For.Message.getMissingMessage(logicClass));
			}

			final GraphLogic logic = constructLogic(applicationContext, logicClass);

			logicsMap.put(anno.value(), as(anno.value(), logic));

			if (logger.isDebugEnabled()) {
				logger.debug("Contributing {}", logic.getLoggableName());
			}
		}

		return logicsMap;
	}

	@SuppressWarnings("rawtypes")
	private GraphLogic constructLogic(ApplicationContext applicationContext, Class<GraphLogic> logicClass)
			throws Exception {
		// @formatter:off
		return Utils.declare(logicClass)
				.then(this::locateConstructor)
					.prepend(applicationContext)
				.then(this::construct)
				.get();
		// @formatter:on
	}

	@SuppressWarnings("rawtypes")
	private GraphLogic construct(ApplicationContext applicationContext, Constructor constructor)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return GraphLogic.class.cast(constructor
				.newInstance(Stream.of(constructor.getParameterTypes()).map(applicationContext::getBean).toArray()));
	}

	@SuppressWarnings("rawtypes")
	private Constructor locateConstructor(Class<GraphLogic> logicClass) {
		Constructor<?>[] constructors = logicClass.getConstructors();

		if (constructors.length == 1) {
			return constructors[0];
		}

		for (Constructor<?> constructor : constructors) {
			if (constructor.isAnnotationPresent(Autowired.class)) {
				return constructor;
			}
		}

		throw new IllegalArgumentException(String.format("Unable to locate any non-arg nor @%s construct in %s type %s",
				Autowired.class.getSimpleName(), GraphLogic.class.getSimpleName(), logicClass));
	}

	@SuppressWarnings("rawtypes")
	private Map<Class, LinkedHashSet<LogicEntry>> addFixedLogics(ApplicationContext applicationContext,
			Map<Class, LogicEntry> contributions) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Adding fixed logics");
		}

		final Map<Class, LinkedHashSet<LogicEntry>> joinedLogic = new HashMap<>();
		// @formatter:off
		final HandledConsumer<Collection<Entry<Class, LogicEntry>>, Exception> finalContributionBuilder = 
			(logicEntries) -> {
				for (final Entry<Class, LogicEntry> logicEntry : logicEntries) {
					final Class resourceType = logicEntry.getKey();
					final LogicEntry logic = logicEntry.getValue();
	
					if (joinedLogic.containsKey(resourceType)) {
						joinedLogic.get(resourceType).add(logic);
						continue;
					}
					
					final LinkedHashSet<LogicEntry> newLogicSet = new LinkedHashSet<>();
					
					newLogicSet.add(logic);
					joinedLogic.put(resourceType, newLogicSet);
				}
			};
		// @formatter:on
		final Collection<Entry<Class, Entry<Class, GraphLogic>>> fixedLogics = getFixedLogics(applicationContext);
		finalContributionBuilder.accept(fixedLogics.stream()
				.map(entry -> Map.entry(entry.getKey(), this.as(entry.getKey(), entry.getValue().getValue())))
				.collect(Collectors.toList()));
		finalContributionBuilder.accept(contributions.entrySet());

		for (final Entry<Class, Entry<Class, GraphLogic>> entry : fixedLogics) {
			System.out.println(String.format("%s:\t%s", entry.getKey().getSimpleName(), String.format("%s(%s)",
					entry.getValue().getKey().getSimpleName(), entry.getValue().getValue().getLoggableName())));
		}

		return joinedLogic;
	}

	@SuppressWarnings("rawtypes")
	private Map<Class, LinkedHashSet<LogicEntry>> formInheritance(Map<Class, LinkedHashSet<LogicEntry>> joinedLogics,
			DomainResourceContext resourceContext, Supplier<GraphLogic> noopSupplier) {
		if (logger.isTraceEnabled()) {
			logger.trace("Building inheritance");
		}

		final Map<Class, LinkedHashSet<LogicEntry>> scopedLogicCollections = new HashMap<>();
		/*
		 * Construct the logic inheritance once per resource type
		 **/
		for (final DomainResourceGraph<?> graph : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toGraphsSet())) {
			final Class<?> resourceType = graph.getResourceType();

			if (scopedLogicCollections.containsKey(resourceType)) {
				continue;
			}

			final Deque<Class<? extends DomainResource>> inheritance = graph.getClassInheritance();
			// reflect the logics collected scoped to the current resourceType
			final LinkedHashSet<LogicEntry> logicEntrySet = new LinkedHashSet<>();
			/* now from the top */
			while (!inheritance.isEmpty()) {
				/* make it drop */
				final Class<? extends DomainResource> currentType = inheritance.pop();
				// if the logics for this type have already been registered,
				// put those into the logic set of resourceType
				if (scopedLogicCollections.containsKey(currentType)) {
					logicEntrySet.addAll(scopedLogicCollections.get(currentType));
					continue;
				}
				// else, if there are provided logics in joinedLogics, scope them into
				// the currentType logics, otherwise, scope a NO_OP logic
				scopedLogicCollections.put(currentType,
						joinedLogics.containsKey(currentType) ? joinedLogics.get(currentType) : with(noopSupplier));
				// ultimately put the above into the logic set of resourceType as well
				logicEntrySet.addAll(scopedLogicCollections.get(currentType));
			}

			scopedLogicCollections.put(resourceType, logicEntrySet);
		}

		return scopedLogicCollections;
	}

	@SuppressWarnings("rawtypes")
	private Map<Class, LinkedHashSet<LogicEntry>> removeNoopLogics(
			Map<Class, LinkedHashSet<LogicEntry>> constructedLogics, DomainResourceContext resourceContext,
			Supplier<GraphLogic> noopSupplier) {
		// @formatter:off
		return constructedLogics.entrySet()
			.stream()
			.map(entry -> Map.entry(
					entry.getKey(),
					entry.getValue().size() > 1
						? entry.getValue()
							.stream()
							.filter(logicEntry -> !logicEntry.getValue().equals(noopSupplier.get()))
							.<LinkedHashSet<LogicEntry>>collect(LinkedHashSet::new, (set, logic) -> set.add(logic), LinkedHashSet::addAll)
						: entry.getValue()))
			.collect(CollectionHelper.toMap());
		// @formatter:on
	}

	@SuppressWarnings("rawtypes")
	private LinkedHashSet<LogicEntry> with(Supplier<GraphLogic> noopSupplier) {
		return new LinkedHashSet<>(List.of(as(DomainResource.class, noopSupplier.get())));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Class, GraphLogic> join(Map<Class, LinkedHashSet<LogicEntry>> constructedLogics) {
		if (logger.isTraceEnabled()) {
			logger.trace("Joining logics");
		}
		// @formatter:off
		return constructedLogics.entrySet()
				.stream()
				.map(entry -> Map.entry(
						entry.getKey(),
						entry.getValue()
							.stream()
							.map(Entry::getValue)
							.reduce((product, logic) -> product.and(logic))
							.get()))
				.collect(CollectionHelper.toMap());
		// @formatter:on
	}

	public static interface FixedLogic {
	}

	@SuppressWarnings("rawtypes")
	private class LogicEntry implements Entry<Class, GraphLogic> {

		private final Class type;
		private final GraphLogic logic;

		public LogicEntry(Class type, GraphLogic walker) {
			this.type = type;
			this.logic = walker;
		}

		@Override
		public Class getKey() {
			return type;
		}

		@Override
		public GraphLogic getValue() {
			return logic;
		}

		@Override
		public GraphLogic setValue(GraphLogic value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;

			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + ((logic == null) ? 0 : logic.hashCode());

			return result;
		}

		@Override
		public boolean equals(Object obj) {
			final LogicEntry other = (LogicEntry) obj;

			return this.type.equals(other.type) && this.logic.equals(other.logic);
		}

	}

	@SuppressWarnings("rawtypes")
	private LogicEntry as(Class key, GraphLogic logic) {
		return new LogicEntry(key, logic);
	}

}
