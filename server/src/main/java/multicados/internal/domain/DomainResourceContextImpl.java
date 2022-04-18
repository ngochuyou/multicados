/**
 * 
 */
package multicados.internal.domain;

import static multicados.internal.helper.Utils.declare;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextManager;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.metadata.DomainResourceMetadataImpl;
import multicados.internal.domain.tuplizer.DomainEntityTuplizer;
import multicados.internal.domain.tuplizer.DomainResourceTuplizer;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceContextImpl implements DomainResourceContext {

	private final DomainResourceGraph<DomainResource> resourceGraph;

	private final Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatasMap;
	private final Map<Class<? extends DomainResource>, DomainResourceTuplizer<? extends DomainResource>> tuplizersMap;

	public DomainResourceContextImpl() throws Exception {
		// @formatter:off
		resourceGraph = declare(scan())
				.then(this::buildGraph)
				.identical(this::sealGraph)
				.get();
		metadatasMap = declare(resourceGraph)
				.then(resourceGraph -> resourceGraph.collect(DomainResourceGraphCollectors.toTypesSet()))
				.then(this::buildMetadatas)
				.then(Collections::unmodifiableMap)
				.get();
		tuplizersMap = declare(resourceGraph)
				.then(resourceGraph -> resourceGraph.collect(DomainResourceGraphCollectors.toTypesSet()))
				.then(this::buildTuplizers)
				.then(Collections::unmodifiableMap)
				.get();
		// @formatter:on
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Class<? extends DomainResource>, DomainResourceTuplizer<? extends DomainResource>> buildTuplizers(
			Collection<Class<DomainResource>> entityTypes) throws Exception {
		final Logger logger = LoggerFactory.getLogger(DomainResourceContextImpl.class);

		logger.trace("Building {}(s)", DomainResourceTuplizer.class.getSimpleName());

		Map<Class<? extends DomainResource>, DomainResourceTuplizer<? extends DomainResource>> tuplizers = new HashMap<>();

		for (Class type : entityTypes) {
			if (tuplizers.containsKey(type)) {
				continue;
			}

			if (Entity.class.isAssignableFrom(type) && !Modifier.isAbstract(type.getModifiers())) {
				logger.trace("HBM {}", type.getName());

				tuplizers.put(type, new DomainEntityTuplizer<>(type, getMetadata(type),
						ContextManager.getBean(SessionFactoryImplementor.class)));
				continue;
			}
		}

		return tuplizers;
	}

	private Set<BeanDefinition> scan() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		final Logger logger = LoggerFactory.getLogger(DomainResourceContextImpl.class);

		logger.trace("Scanning for {}", DomainResource.class.getSimpleName());
		scanner.addIncludeFilter(new AssignableTypeFilter(DomainResource.class));

		Set<BeanDefinition> candidates = scanner.findCandidateComponents(Settings.BASE_PACKAGE);

		logger.trace("Found {} candidate(s)", candidates.size());

		return candidates;
	}

	@SuppressWarnings("unchecked")
	private DomainResourceGraph<DomainResource> buildGraph(Set<BeanDefinition> beanDefs) throws ClassNotFoundException {
		final Logger logger = LoggerFactory.getLogger(DomainResourceContextImpl.class);

		logger.trace("Building {}", DomainResourceGraph.class.getSimpleName());
		// @formatter:off
		DomainResourceGraphImpl<DomainResource> resourceGraph = new DomainResourceGraphImpl<>(null, DomainResource.class, Set.of(
				new DomainResourceGraphImpl<>(
						IdentifiableResource.class,
						new HashSet<>(List.of(
								new DomainResourceGraphImpl<>(EncryptedIdentifierResource.class),
								new DomainResourceGraphImpl<>(Entity.class)))),
				new DomainResourceGraphImpl<>(NamedResource.class),
				new DomainResourceGraphImpl<>(PermanentResource.class),
				new DomainResourceGraphImpl<>(SpannedResource.class),
				new DomainResourceGraphImpl<>(AuditableResource.class)));
		// @formatter:on
		for (BeanDefinition beanDef : beanDefs) {
			Class<DomainResource> clazz = (Class<DomainResource>) Class.forName(beanDef.getBeanClassName());

			logger.trace("{} type [{}]", DomainResource.class.getSimpleName(), clazz.getName());

			Stack<?> stack = TypeHelper.getClassStack(clazz);

			while (!stack.isEmpty()) {
				resourceGraph.add((Class<DomainResource>) stack.pop());
			}
		}

		return resourceGraph;
	}

	private <T extends DomainResource> void sealGraph(DomainResourceGraph<T> graph) throws Exception {
		final Logger logger = LoggerFactory.getLogger(DomainResourceContextImpl.class);

		logger.trace("Sealing {}", DomainResourceGraph.class.getSimpleName());

		graph.forEach(node -> node.doAfterContextBuild());
	}

	private Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> buildMetadatas(
			Collection<Class<DomainResource>> resourceTypes) throws Exception {
		final Logger logger = LoggerFactory.getLogger(DomainResourceContextImpl.class);

		logger.trace("Building {}(s)", DomainResourceMetadata.class.getSimpleName());

		Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatasMap = new HashMap<>(
				0);
//		ObservableMetadataEntries metadataEntries = new ObservableMetadataEntriesImpl();
		// we use iterator for exception handling
		for (Class<? extends DomainResource> resourceType : resourceTypes) {
			if (metadatasMap.containsKey(resourceType) || Modifier.isInterface(resourceType.getModifiers())) {
				continue;
			}

			DomainResourceMetadataImpl<? extends DomainResource> metadata = new DomainResourceMetadataImpl<>(
					resourceType, this, metadatasMap);

//			metadataEntries.notify(metadata);
			metadatasMap.put(resourceType, metadata);
		}

//		metadataEntries.close();
		return metadatasMap;
	}

	@Override
	public void summary() throws Exception {
		final Logger logger = LoggerFactory.getLogger(DomainResourceContextImpl.class);

		if (!logger.isDebugEnabled()) {
			return;
		}

		logger.debug("\n{}:\n\s\s\s{}", DomainResourceGraph.class.getSimpleName(), visualizeGraph(resourceGraph, 0));
		logger.debug("\n{}", metadatasMap.values().stream().map(Object::toString).collect(Collectors.joining("\n")));
	}

	private String visualizeGraph(DomainResourceGraph<? extends DomainResource> node, int indentation)
			throws Exception {
		StringBuilder builder = new StringBuilder();
		// @formatter:off
		builder.append(String.format("%s%s\n\s\s\s",
				indentation != 0 ? String.format("%s%s",
						IntStream.range(0, indentation).mapToObj(index -> "\s\s\s").collect(Collectors.joining(StringHelper.EMPTY_STRING)),
						"|__") : StringHelper.EMPTY_STRING,
				node.getResourceType().getSimpleName()));
		// @formatter:on
		if (CollectionHelper.isEmpty(node.getChildrens())) {
			return builder.toString();
		}

		node.getChildrens().forEach(children -> {
			try {
				builder.append(visualizeGraph(children, indentation + 1));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return builder.toString();
	}

//	@SuppressWarnings("unchecked")
//	private <T extends DomainResource> DomainResourceGraph<T> locateGraph(DomainResourceGraph<? super T> root,
//			Class<T> resourceType) {
//		if (root.getResourceType().equals(resourceType)) {
//			return (DomainResourceGraph<T>) root;
//		}
//
//		if (CollectionHelper.isEmpty(root.getChildrens())) {
//			return null;
//		}
//
//		return root.getChildrens().stream()
//				.map(node -> locateGraph((DomainResourceGraph<? super T>) node, resourceType)).filter(Objects::nonNull)
//				.findFirst().orElse(null);
//	}

	@Override
	public DomainResourceGraph<DomainResource> getResourceGraph() {
		return resourceGraph;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends DomainResource> DomainResourceMetadata<T> getMetadata(Class<T> resourceType) {
		return (DomainResourceMetadata<T>) metadatasMap.get(resourceType);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends DomainResource> DomainResourceTuplizer<T> getTuplizer(Class<T> resourceType) {
		return (DomainResourceTuplizer<T>) tuplizersMap.get(resourceType);
	}

//	private class ObservableMetadataEntriesImpl implements ObservableMetadataEntries {
//
//		private Utils.Access access = new Access() {};
//
//		@SuppressWarnings("rawtypes")
//		private final Map<Class<? extends DomainResource>, List<MetadataEntryObserver>> observers = new HashMap<>();
//
//		public synchronized <D extends DomainResource> void subscribe(Class<D> expectingType,
//				MetadataEntryObserver<D> observer) throws IllegalAccessException {
//			Assert.notNull(access, Access.CLOSED_MESSAGE);
//			LoggerFactory.getLogger(DomainResourceContextImpl.ObservableMetadataEntries.class).trace(
//					"Registering new {} for type {}", MetadataEntryObserver.class.getSimpleName(), expectingType);
//
//			if (!observers.containsKey(expectingType)) {
//				observers.put(expectingType, Stream.of(observer).collect(Collectors.toList()));
//				return;
//			}
//
//			observers.get(expectingType).add(observer);
//		}
//
//		@SuppressWarnings("unchecked")
//		public synchronized <D extends DomainResource> void notify(DomainResourceMetadata<D> metadata) {
//			Assert.notNull(access, Access.CLOSED_MESSAGE);
//			final Class<D> type = metadata.getResourceType();
//
//			LoggerFactory.getLogger(DomainResourceContextImpl.ObservableMetadataEntries.class)
//					.trace("Invoking signals for observers of type {}", type);
//
//			for (MetadataEntryObserver<D> observer : observers.get(type)) {
//				observer.notify(metadata);
//			}
//
//			observers.remove(type);
//		}
//
//		@Override
//		public void close() throws IOException {
//			LoggerFactory.getLogger(DomainResourceContextImpl.ObservableMetadataEntries.class)
//					.trace(Access.getClosingMessage(this));
//			access = null;
//		}
//
//	}

}
