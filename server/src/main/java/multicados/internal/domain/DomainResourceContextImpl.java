/**
 *
 */
package multicados.internal.domain;

import static multicados.internal.helper.Utils.declare;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.TypeFilter;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.metadata.DomainResourceAttributesMetadata;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.metadata.DomainResourceMetadataBuilder;
import multicados.internal.domain.metadata.DomainResourceMetadataBuilderImpl;
import multicados.internal.domain.metadata.HibernateDomainResourceMetadataBuilder;
import multicados.internal.domain.tuplizer.AbstractDomainResourceTuplizer;
import multicados.internal.domain.tuplizer.AccessorFactory.Accessor;
import multicados.internal.domain.tuplizer.DomainResourceTuplizer;
import multicados.internal.domain.tuplizer.DomainResourceTuplizerImpl;
import multicados.internal.domain.tuplizer.HibernateResourceTuplizer;
import multicados.internal.file.domain.FileResource;
import multicados.internal.file.engine.FileManagement;
import multicados.internal.file.engine.FileResourceSessionFactory;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceContextImpl extends ContextBuilder.AbstractContextBuilder implements DomainResourceContext {

	private static final Logger logger = LoggerFactory.getLogger(DomainResourceContextImpl.class);

	private final DomainResourceGraph<DomainResource> resourceGraph;

	private final Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatasMap;
	private final Map<Class<? extends DomainResource>, DomainResourceTuplizer<? extends DomainResource>> tuplizersMap;

	@Autowired
	public DomainResourceContextImpl(SessionFactory sessionFactory, FileManagement fileManagement) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Constructing {}", DomainResourceContextImpl.class.getName());
		}

		resourceGraph = new GraphBuilder().build();

		final SessionFactoryImplementor sfi = sessionFactory.unwrap(SessionFactoryImplementor.class);
		final FileResourceSessionFactory fileResourceSessionFactory = fileManagement.getSessionFactory();

		metadatasMap = new MetadataBuilder(resourceGraph, sfi, fileResourceSessionFactory).build();
		tuplizersMap = new TuplizerBuilder(resourceGraph, sfi, fileResourceSessionFactory).build();
	}

	@Override
	public DomainResourceGraph<DomainResource> getResourceGraph() {
		return resourceGraph;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends DomainResource> DomainResourceMetadata<T> getMetadata(Class<T> resourceType) {
		return (DomainResourceAttributesMetadata<T>) metadatasMap.get(resourceType);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends DomainResource> DomainResourceTuplizer<T> getTuplizer(Class<T> resourceType) {
		return (DomainResourceTuplizer<T>) tuplizersMap.get(resourceType);
	}

	private boolean isSupportedByHBM(final Class<? extends DomainResource> resourceType) {
		return !Modifier.isAbstract(resourceType.getModifiers()) && !Modifier.isInterface(resourceType.getModifiers());
	}

	@Override
	public void summary() {
		logger.info("\n{}", visualizeGraph(resourceGraph, 1));
		logger.info("\n{}", metadatasMap.values().stream().map(Object::toString).collect(Collectors.joining("\n")));
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

	private class TuplizerBuilder {

		private final Map<Class<? extends DomainResource>, DomainResourceTuplizer<? extends DomainResource>> tuplizers = new HashMap<>();

		private final DomainResourceGraph<DomainResource> resourceGraph;
		private final SessionFactoryImplementor sessionFactory;
		private final FileResourceSessionFactory fileResourceSessionFactory;

		private final Map<AccessorKey, Accessor> accessorsCache = new HashMap<>();
		private final Map<Class<? extends DomainResource>, AbstractDomainResourceTuplizer<? extends DomainResource>> parentTuplizersCache = new HashMap<>();

		public TuplizerBuilder(DomainResourceGraph<DomainResource> resourceGraph, SessionFactoryImplementor sfi,
				FileResourceSessionFactory fileResourceSessionFactory) {
			this.resourceGraph = resourceGraph;
			this.sessionFactory = sfi;
			this.fileResourceSessionFactory = fileResourceSessionFactory;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private Map<Class<? extends DomainResource>, DomainResourceTuplizer<? extends DomainResource>> build()
				throws Exception {
			if (logger.isTraceEnabled()) {
				logger.trace("Building tuplizers");
			}

			final BiFunction<Class<?>, String, Accessor> cachedAccessorProvider = this::findCachedAccessor;
			final Utils.TriConsummer<Class<?>, String, Accessor> accessorEntryConsumer = this::cacheAccessor;
			final Function<Class<? extends DomainResource>, AbstractDomainResourceTuplizer<?>> parentTuplizerProvider = this::locateParentTuplizer;

			for (final Class<? extends DomainResource> resourceType : resourceGraph
					.collect(DomainResourceGraphCollectors.toTypesSet())) {
				final DomainResourceMetadata<? extends DomainResource> metadata = getMetadata(resourceType);

				if (logger.isTraceEnabled()) {
					logger.trace("Resolving {} for resource type {}", DomainResourceTuplizer.class.getName(),
							resourceType.getName());
				}

				if (isSupportedByHBM(resourceType)) {
					if (Entity.class.isAssignableFrom(resourceType)) {
						// @formatter:off
						tuplizers.put(resourceType,
								new HibernateResourceTuplizer(
										metadata,
										sessionFactory,
										cachedAccessorProvider,
										accessorEntryConsumer));
						// @formatter:on
						continue;
					}

					if (FileResource.class.isAssignableFrom(resourceType)) {
						// @formatter:off
						tuplizers.put(resourceType,
								new HibernateResourceTuplizer(
										metadata,
										fileResourceSessionFactory,
										cachedAccessorProvider,
										accessorEntryConsumer));
						// @formatter:on
						continue;
					}
				}
				// @formatter:off
				tuplizers.put(resourceType,
						new DomainResourceTuplizerImpl<>(
								metadata,
								cachedAccessorProvider,
								accessorEntryConsumer,
								parentTuplizerProvider));
				// @formatter:on
			}

			return tuplizers;
		}

		private AbstractDomainResourceTuplizer<?> locateParentTuplizer(Class<? extends DomainResource> resourceType) {
			if (parentTuplizersCache.containsKey(resourceType)) {
				return parentTuplizersCache.get(resourceType);
			}

			final DomainResourceGraph<?> graph = resourceGraph.locate(resourceType);

			if (CollectionHelper.isEmpty(graph.getParents())) {
				return null;
			}

			for (final DomainResourceGraph<?> possibleParentGraph : graph.getParents()) {
				final Class<?> parentType = possibleParentGraph.getResourceType();

				if (Modifier.isInterface(parentType.getModifiers())) {
					continue;
				}

				final AbstractDomainResourceTuplizer<?> parentTuplizer = (AbstractDomainResourceTuplizer<?>) tuplizers
						.get(parentType);

				parentTuplizersCache.put(resourceType, parentTuplizer);

				return parentTuplizer;
			}

			return null;
		}

		private Accessor findCachedAccessor(Class<?> ownerType, String attributePath) {
			final AccessorKey key = new AccessorKey(ownerType, attributePath);

			if (logger.isTraceEnabled()) {
				if (accessorsCache.containsKey(key)) {
					logger.trace("Using cached accessor for key [{}]", key);
				}
			}

			return accessorsCache.get(key);
		}

		private void cacheAccessor(Class<?> ownerType, String attributePath, Accessor accessor) {
			final AccessorKey key = new AccessorKey(ownerType, attributePath);

			if (logger.isTraceEnabled()) {
				logger.trace("Caching accessor with key [{}]", key);
			}

			accessorsCache.put(new AccessorKey(ownerType, attributePath), accessor);
		}

		private class AccessorKey {

			private final Class<?> resourceType;
			private final String path;

			public AccessorKey(Class<?> resourceType, String path) {
				this.resourceType = resourceType;
				this.path = path;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;

				result = prime * result + getEnclosingInstance().hashCode();
				result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
				result = prime * result + ((path == null) ? 0 : path.hashCode());

				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if ((obj == null) || (getClass() != obj.getClass()))
					return false;

				final AccessorKey other = (AccessorKey) obj;

				if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
					return false;
				if (path == null) {
					if (other.path != null)
						return false;
				} else if (!path.equals(other.path))
					return false;
				if (resourceType == null) {
					if (other.resourceType != null)
						return false;
				} else if (!resourceType.equals(other.resourceType))
					return false;

				return true;
			}

			private TuplizerBuilder getEnclosingInstance() {
				return TuplizerBuilder.this;
			}

			@Override
			public String toString() {
				return String.format("%s#%s", resourceType, path);
			}

		}

	}

	@SuppressWarnings({ "rawtypes" })
	private class MetadataBuilder {

		private final DomainResourceGraph<DomainResource> graph;

		private final Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatas = new HashMap<>();

		private final SessionFactoryImplementor sessionFactory;
		private final FileResourceSessionFactory fileResourceSessionFactory;

		public MetadataBuilder(DomainResourceGraph<DomainResource> graph, SessionFactoryImplementor sessionFactory,
				FileResourceSessionFactory fileResourceSessionFactory) {
			this.graph = graph;
			this.sessionFactory = sessionFactory;
			this.fileResourceSessionFactory = fileResourceSessionFactory;
		}

		@SuppressWarnings("unchecked")
		public Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> build()
				throws Exception {
			if (logger.isTraceEnabled()) {
				logger.trace("Building metadatas", DomainResourceGraph.class.getName());
			}

			final DomainResourceMetadataBuilder nonHibernateResourceMetadataBuilder = new DomainResourceMetadataBuilderImpl();
			final DomainResourceMetadataBuilder datasourceResourceMetadataBuilder = new HibernateDomainResourceMetadataBuilder(
					sessionFactory);
			final DomainResourceMetadataBuilder fileResourceMetadataBuilder = new HibernateDomainResourceMetadataBuilder(
					fileResourceSessionFactory);

			for (final Class resourceType : graph.collect(DomainResourceGraphCollectors.toTypesSet())) {
				if (logger.isTraceEnabled()) {
					logger.trace("Building metadata for type {}", resourceType.getName());
				}

				if (Entity.class.isAssignableFrom(resourceType) && canBePersisted(resourceType)) {
					metadatas.put(resourceType, datasourceResourceMetadataBuilder.build(resourceType, metadatas));
					continue;
				}

				if (FileResource.class.isAssignableFrom(resourceType) && canBePersisted(resourceType)) {
					metadatas.put(resourceType, fileResourceMetadataBuilder.build(resourceType, metadatas));
					continue;
				}

				metadatas.put(resourceType, nonHibernateResourceMetadataBuilder.build(resourceType, metadatas));
				continue;
			}

			return Collections.unmodifiableMap(metadatas);
		}

		private boolean canBePersisted(Class<? extends DomainResource> resourceType) {
			final int modifiers = resourceType.getModifiers();
			return !Modifier.isInterface(resourceType.getModifiers()) && !Modifier.isAbstract(modifiers);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private class GraphBuilder {

		/**
		 * To progressively store the {@link DomainResourceGraph} while building for
		 * quickly locate them
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

		public GraphBuilder() {
			cache.put(DomainResource.class, root);
		}

		public DomainResourceGraph<DomainResource> build() throws Exception {
			if (logger.isTraceEnabled()) {
				logger.trace("Building {}", DomainResourceGraph.class.getName());
			}
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

			if (logger.isTraceEnabled()) {
				logger.trace("New put-graph request of type {}", type.getName());
			}

			final DomainResourceGraph graph = new DomainResourceGraphImpl<>(type);
			final AnnotationMetadata metadata = ScannedGenericBeanDefinition.class.cast(beanDef).getMetadata();

			pushClassStack(metadata);
			pushInterfaceStack(metadata);
			putGraph(type, graph);
		}

		private void pushInterfaceStack(AnnotationMetadata metadata) throws ClassNotFoundException {
			final String[] interfaceClassNames = metadata.getInterfaceNames();

			if (interfaceClassNames.length == 0) {
				return;
			}

			for (final String interfaceClassName : interfaceClassNames) {
				pushParentsToGraph(with(interfaceClassName));
			}
		}

		private void pushClassStack(AnnotationMetadata metadata) throws ClassNotFoundException {
			if (logger.isTraceEnabled()) {
				logger.trace("Pushing class stack of type {} to graph", metadata.getClassName());
			}

			if (metadata.getSuperClassName() == null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Class stack is empty for type {}", metadata.getClassName());
				}

				return;
			}

			pushParentsToGraph(with(metadata.getSuperClassName()));
		}

		private void pushParentsToGraph(Class directParentType) throws ClassNotFoundException {
			final Stack<Class<?>> classStack = TypeHelper.getClassStack(directParentType);

			while (!classStack.isEmpty()) {
				Class<?> currentType = classStack.pop();

				if (!DomainResource.class.isAssignableFrom(currentType) || cache.containsKey(currentType)) {
					continue;
				}

				final DomainResourceGraphImpl parentGraph = new DomainResourceGraphImpl<>(
						(Class<? extends DomainResource>) currentType);

				putGraph(currentType, parentGraph);
			}
		}

		private void putGraph(Class type, DomainResourceGraph graph) throws ClassNotFoundException {
			if (cache.containsKey(type)) {
				if (logger.isTraceEnabled()) {
					logger.trace("{} has already exsited in graph", type.getName());
				}

				return;
			}

			if (logger.isTraceEnabled()) {
				logger.trace("Adding a new graph of type {}", type.getName());
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

		private <T extends DomainResource> Class<T> with(String className) throws ClassNotFoundException {
			return (Class<T>) Class.forName(className);
		}

		private <T extends DomainResource> Class<T> from(BeanDefinition beanDef) throws ClassNotFoundException {
			return with(beanDef.getBeanClassName());
		}

		private <T extends DomainResource> void sealGraph() throws Exception {
			if (logger.isTraceEnabled()) {
				logger.trace("Sealing graph");
			}

			root.forEach(node -> node.doAfterContextBuild());
		}

	}

}
