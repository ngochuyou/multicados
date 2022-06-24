/**
 * 
 */
package multicados.internal.domain;

import static multicados.internal.helper.Utils.declare;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.TypeFilter;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.tuplizer.DomainResourceTuplizer;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceContextImpl extends ContextBuilder.AbstractContextBuilder implements DomainResourceContext {

	private static final Logger logger = LoggerFactory.getLogger(DomainResourceContextImpl.class);

	private final DomainResourceGraph<DomainResource> resourceGraph;

	private final Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatasMap = null;
	private final Map<Class<? extends DomainResource>, DomainResourceTuplizer<? extends DomainResource>> tuplizersMap = null;

	public DomainResourceContextImpl() throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Constructing {}", DomainResourceContextImpl.class.getName());
		}

		resourceGraph = new DomainResourceGraphBuilder().build();
	}

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

//	@Autowired
//	public DomainResourceContextImpl(SessionFactory sessionFactory) throws Exception {
//		if (logger.isTraceEnabled()) {
//			logger.trace("Instantiating {}", DomainResourceContextImpl.class.getName());
//		}
//
//		final SessionFactoryImplementor sfi = sessionFactory.unwrap(SessionFactoryImplementor.class);
////		metadatasMap = declare(resourceGraph, sfi, fileManagement)
////					.map(resourceGraph -> resourceGraph.collect(DomainResourceGraphCollectors.toTypesSet()), HandledFunction.identity(), HandledFunction.identity())
////				.then(this::buildMetadatas)
////				.then(Collections::unmodifiableMap)
////				.get();
////		tuplizersMap = declare(resourceGraph, sfi)
////					.map(resourceGraph -> resourceGraph.collect(DomainResourceGraphCollectors.toTypesSet()), HandledFunction.identity())
////				.then(this::buildTuplizers)
////				.then(Collections::unmodifiableMap)
////				.get();
	//		// @formatter:on
//	}
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	private Map<Class<? extends DomainResource>, DomainResourceTuplizer<? extends DomainResource>> buildTuplizers(
//			Collection<Class<DomainResource>> entityTypes, SessionFactoryImplementor sfi) throws Exception {
//		if (logger.isTraceEnabled()) {
//			logger.trace("Building {}", DomainResourceTuplizer.class.getSimpleName());
//		}
//
//		final Map<Class<? extends DomainResource>, DomainResourceTuplizer<? extends DomainResource>> tuplizers = new HashMap<>();
//
//		for (Class type : entityTypes) {
//			if (tuplizers.containsKey(type)) {
//				continue;
//			}
//
//			if (Entity.class.isAssignableFrom(type) && !Modifier.isAbstract(type.getModifiers())) {
//				tuplizers.put(type, new DomainEntityTuplizer<>(type, getMetadata(type), sfi));
//				continue;
//			}
//		}
//
//		return tuplizers;
//	}
//
//	@SuppressWarnings("unchecked")
//	private DomainResourceGraph<DomainResource> buildGraph(Set<BeanDefinition> beanDefs) throws ClassNotFoundException {
//		if (logger.isTraceEnabled()) {
//			logger.trace("Building {}", DomainResourceGraph.class.getSimpleName());
//		}
//
//		final DomainResourceGraph<DomainResource> resourceGraph = constructGraphRoot();
//
////		for (final BeanDefinition beanDef : beanDefs) {
////			final Class<DomainResource> clazz = (Class<DomainResource>) Class.forName(beanDef.getBeanClassName());
////			final Stack<?> stack = TypeHelper.getClassStack(clazz);
////
////			while (!stack.isEmpty()) {
////				resourceGraph.add((Class<DomainResource>) stack.pop());
////			}
////		}
//
//		return resourceGraph;
//	}
//
//	private <T extends DomainResource> void sealGraph(DomainResourceGraph<T> graph) throws Exception {
//		if (logger.isTraceEnabled()) {
//			logger.trace("Sealing {}", DomainResourceGraph.class.getSimpleName());
//		}
//
//		graph.forEach(node -> node.doAfterContextBuild());
//	}
//
//	private Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> buildMetadatas(
//			Collection<Class<DomainResource>> resourceTypes, SessionFactoryImplementor sfi,
//			FileManagement fileManagement) throws Exception {
//		if (logger.isTraceEnabled()) {
//			logger.trace("Building {}(s)", DomainResourceMetadata.class.getSimpleName());
//		}
//
//		final Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatasMap = new HashMap<>(
//				0);
//
//		for (Class<? extends DomainResource> resourceType : resourceTypes) {
//			if (metadatasMap.containsKey(resourceType) || Modifier.isInterface(resourceType.getModifiers())) {
//				continue;
//			}
//
//			DomainResourceMetadataImpl<? extends DomainResource> metadata = new DomainResourceMetadataImpl<>(
//					resourceType, this, metadatasMap, sfi, fileManagement);
//
//			metadatasMap.put(resourceType, metadata);
//		}
//
//		return metadatasMap;
//	}
//
//	@Override
//	public void summary() {
//		if (!logger.isDebugEnabled()) {
//			return;
//		}
//
//		logger.info("\n{}:\n\s\s\s{}", DomainResourceGraph.class.getSimpleName(), visualizeGraph(resourceGraph, 0));
////		logger.info("\n{}", metadatasMap.values().stream().map(Object::toString).collect(Collectors.joining("\n")));
//	}
//
//	private String visualizeGraph(DomainResourceGraph<? extends DomainResource> node, int indentation) {
//		final StringBuilder builder = new StringBuilder();
//		// @formatter:off
//		builder.append(String.format("%s%s[%d]%s\n\s\s\s",
//				indentation != 0 ? String.format("%s%s",
//						IntStream.range(0, indentation).mapToObj(index -> "\s\s\s").collect(Collectors.joining(StringHelper.EMPTY_STRING)),
//						"|__") : StringHelper.EMPTY_STRING,
//				node.getResourceType().getSimpleName(),
//				node.getDepth(),
//				node.getParents() != null ? node.getParents().stream().map(p -> p.getResourceType().getSimpleName()).collect(Collectors.joining(StringHelper.COMMON_JOINER)) : ""));
//		// @formatter:on
//		if (CollectionHelper.isEmpty(node.getChildrens())) {
//			return builder.toString();
//		}
//
//		for (final DomainResourceGraph<?> children : node.getChildrens()) {
//			builder.append(visualizeGraph(children, indentation + 1));
//		}
//
//		return builder.toString();
//	}
//
//	@Override
//	public DomainResourceGraph<DomainResource> getResourceGraph() {
//		return resourceGraph;
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public <T extends DomainResource> DomainResourceMetadata<T> getMetadata(Class<T> resourceType) {
//		return (DomainResourceMetadata<T>) metadatasMap.get(resourceType);
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public <T extends DomainResource> DomainResourceTuplizer<T> getTuplizer(Class<T> resourceType) {
//		return (DomainResourceTuplizer<T>) tuplizersMap.get(resourceType);
//	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private class DomainResourceGraphBuilder {

		/**
		 * To progressively store the {@link DomainResourceGraph} while building
		 */
		private final Map<Class, DomainResourceGraph> cache = new HashMap<>();
		private final DomainResourceGraph<DomainResource> root = new DomainResourceGraphImpl<>(DomainResource.class);

		private final TypeFilter beanIsDomainResource = (metadataReader, metadataReaderFactory) -> {
			try {
				return DomainResource.class
						.isAssignableFrom(Class.forName(metadataReader.getClassMetadata().getClassName()));
			} catch (ClassNotFoundException any) {
				if (logger.isErrorEnabled()) {
					logger.error("Error while scanning for root interfaces: {}", any.getMessage());
				}

				return false;
			}
		};

		public DomainResourceGraphBuilder() {
			cache.put(DomainResource.class, root);
		}

		public DomainResourceGraph<DomainResource> build() throws Exception {
			// @formatter:off
			declare(scanForRootInterfaces())
				.consume(this::constructGraphUsingRootInterfaces);
			
			declare(scanForImplementations())
				.consume(this::addImplementationsToGraph);
			
			sealGraph();
			
			return root;
			// @formatter:on
		}

		private void addImplementationsToGraph(Set<BeanDefinition> implementationDefs)
				throws ClassNotFoundException, Exception {
			if (logger.isTraceEnabled()) {
				logger.trace("Adding implementations to graph");
			}

			pushToGraph(implementationDefs);
		}

		private Set<BeanDefinition> scanForImplementations() {
			if (logger.isDebugEnabled()) {
				logger.debug("Scanning for implementations in package {}", Settings.DOMAIN_SPECIFIC_BASE_PACKAGE);
			}

			final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
					false) {
				@Override
				protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
					return true;
				}
			};

			scanner.addIncludeFilter(beanIsDomainResource);

			return scanner.findCandidateComponents(Settings.DOMAIN_SPECIFIC_BASE_PACKAGE);
		}

		private void putGraph(BeanDefinition beanDef) throws ClassNotFoundException {
			final Class type = from(beanDef);
			final DomainResourceGraph graph = new DomainResourceGraphImpl<>(type);
			final AnnotationMetadata metadata = ScannedGenericBeanDefinition.class.cast(beanDef).getMetadata();

			pushClassStack(metadata, graph);
			pushInterfaceStack(metadata, graph);
			putGraph(type, graph);
		}

		private void pushInterfaceStack(AnnotationMetadata metadata, DomainResourceGraph graph)
				throws ClassNotFoundException {
			String[] interfaceClassNames = metadata.getInterfaceNames();

			if (interfaceClassNames.length == 0) {
				return;
			}

			for (String interfaceClassName : interfaceClassNames) {
				pushParentsToGraph(from(interfaceClassName), graph);
			}
		}

		private void pushClassStack(AnnotationMetadata metadata, DomainResourceGraph graph)
				throws ClassNotFoundException {
			if (metadata.getSuperClassName() == null) {
				return;
			}

			Class currentClass = from(metadata.getSuperClassName());

			pushParentsToGraph(currentClass, graph);
		}

		private void pushParentsToGraph(Class currentClass, DomainResourceGraph graph) throws ClassNotFoundException {
			do {
				try {
					if (!DomainResource.class.isAssignableFrom(currentClass)) {
						continue;
					}

					if (cache.containsKey(currentClass)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Adding {} to exsiting graph type {}", graph.getResourceType(), currentClass);
						}

						cache.get(currentClass).add(graph);
						return;
					}

					putGraph(currentClass, new DomainResourceGraphImpl<>(currentClass));
				} finally {
					currentClass = currentClass.getSuperclass();
				}
			} while (currentClass != null);
		}

		private void putGraph(Class type, DomainResourceGraph graph) throws ClassNotFoundException {
			if (cache.containsKey(type)) {
				if (logger.isDebugEnabled()) {
					logger.debug("{} has already exsited in graph", type.getName());
				}

				return;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Adding a new graph of type {}", type.getName());
			}

			cache.put(type, graph);
			root.add(graph);
		}

		private void constructGraphUsingRootInterfaces(Set<BeanDefinition> interfacesDefs) throws Exception {
			if (logger.isTraceEnabled()) {
				logger.trace("Constructing Graph using root interfaces");
			}

			pushToGraph(interfacesDefs);
		}

		private void pushToGraph(Set<BeanDefinition> beanDefs) throws Exception, ClassNotFoundException {
			for (final BeanDefinition beanDef : beanDefs) {
				putGraph(beanDef);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Current Graph state:\n{}", visualizeGraph(root, 1));
			}
		}

		private Set<BeanDefinition> scanForRootInterfaces() {
			if (logger.isTraceEnabled()) {
				logger.trace("Scanning for root interfaces in package {}", Settings.INTERNAL_BASE_PACKAGE);
			}

			final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
					false) {
				@Override
				protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
					return beanDefinition.getMetadata().isInterface();
				}
			};

			scanner.addIncludeFilter(beanIsDomainResource);

			return scanner.findCandidateComponents(Settings.INTERNAL_BASE_PACKAGE);
		}

		private <T extends DomainResource> Class<T> from(String className) throws ClassNotFoundException {
			return (Class<T>) Class.forName(className);
		}

		private <T extends DomainResource> Class<T> from(BeanDefinition beanDef) throws ClassNotFoundException {
			return from(beanDef.getBeanClassName());
		}

		private String visualizeGraph(DomainResourceGraph<? extends DomainResource> node, int indentation) {
			final StringBuilder builder = new StringBuilder();
			// @formatter:off
			builder.append(String.format("%s%s: %s\n",
					indentation != 0 ? String.format("%s%s",
							IntStream.range(0, indentation).mapToObj(index -> "\s\s\s").collect(Collectors.joining(StringHelper.EMPTY_STRING)),
							"|__") : StringHelper.EMPTY_STRING,
					node.getResourceType().getSimpleName(),
					node.getParents() != null
						? node.getParents().stream().map(p -> p.getResourceType().getSimpleName()).collect(Collectors.joining(StringHelper.COMMON_JOINER))
								: "<ROOT>"));
			// @formatter:on
			if (CollectionHelper.isEmpty(node.getChildrens())) {
				return builder.toString();
			}

			for (final DomainResourceGraph<?> children : node.getChildrens()) {
				builder.append(visualizeGraph(children, indentation + 1));
			}

			return builder.toString();
		}

		private <T extends DomainResource> void sealGraph() throws Exception {
			if (logger.isTraceEnabled()) {
				logger.trace("Sealing graph");
			}

			root.forEach(node -> node.doAfterContextBuild());
		}

	}

}
