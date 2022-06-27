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
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.metadata.DomainResourceMetadataBuilder;
import multicados.internal.domain.metadata.DomainResourceMetadataBuilderImpl;
import multicados.internal.domain.metadata.HibernateDomainResourceMetadataBuilder;
import multicados.internal.domain.tuplizer.DomainResourceTuplizer;
import multicados.internal.file.domain.FileResource;
import multicados.internal.file.engine.FileManagement;
import multicados.internal.file.engine.FileResourceSessionFactory;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceContextImpl extends ContextBuilder.AbstractContextBuilder implements DomainResourceContext {

	private static final Logger logger = LoggerFactory.getLogger(DomainResourceContextImpl.class);

	private final DomainResourceGraph<DomainResource> resourceGraph;

	private final Map<Class<?>, DomainResourceMetadata<?>> metadatasMap;
	private final Map<Class<?>, DomainResourceTuplizer<?>> tuplizersMap = null;

	@Autowired
	public DomainResourceContextImpl(SessionFactory sessionFactory, FileManagement fileManagement) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Constructing {}", DomainResourceContextImpl.class.getName());
		}

		resourceGraph = new GraphBuilder().build();
		metadatasMap = new MetadataBuilder(resourceGraph, sessionFactory, fileManagement.getSessionFactory()).build();
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

	@Override
	public void summary() {
		logger.info("\n{}", metadatasMap.values().stream().map(Object::toString).collect(Collectors.joining("\n")));
	}

	@SuppressWarnings({ "rawtypes" })
	private class MetadataBuilder {

		private final DomainResourceGraph<DomainResource> graph;

		private final Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatas = new HashMap<>();

		private final SessionFactoryImplementor sessionFactory;
		private final FileResourceSessionFactory fileResourceSessionFactory;

		public MetadataBuilder(DomainResourceGraph<DomainResource> graph, SessionFactory sessionFactory,
				FileResourceSessionFactory fileResourceSessionFactory) {
			this.graph = graph;
			this.sessionFactory = sessionFactory.unwrap(SessionFactoryImplementor.class);
			this.fileResourceSessionFactory = fileResourceSessionFactory;
		}

		@SuppressWarnings("unchecked")
		public Map<Class<?>, DomainResourceMetadata<?>> build() throws Exception {
			if (logger.isTraceEnabled()) {
				logger.trace("Building metadatas", DomainResourceGraph.class.getName());
			}

			final DomainResourceMetadataBuilder nonHibernateResourceMetadataBuilder = new DomainResourceMetadataBuilderImpl();
			final DomainResourceMetadataBuilder datasourceResourceMetadataBuilder = new HibernateDomainResourceMetadataBuilder(
					sessionFactory);
			final DomainResourceMetadataBuilder fileResourceMetadataBuilder = new HibernateDomainResourceMetadataBuilder(
					fileResourceSessionFactory);

			for (final Class resourceType : graph.collect(DomainResourceGraphCollectors.toTypesSet())) {
				if (logger.isDebugEnabled()) {
					logger.debug("Building metadata for type {}", resourceType.getName());
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

			if (logger.isDebugEnabled()) {
				logger.debug("New put-graph request of type {}", type.getName());
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
				pushParentsToGraph(from(interfaceClassName));
			}
		}

		private void pushClassStack(AnnotationMetadata metadata) throws ClassNotFoundException {
			if (logger.isDebugEnabled()) {
				logger.debug("Pushing class stack of type {} to graph", metadata.getClassName());
			}

			if (metadata.getSuperClassName() == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Class stack is empty for type {}", metadata.getClassName());
				}

				return;
			}

			pushParentsToGraph(from(metadata.getSuperClassName()));
		}

		private void pushParentsToGraph(Class directParentType) throws ClassNotFoundException {
			final Stack<Class<?>> classStack = TypeHelper.getClassStack(directParentType);

			while (!classStack.isEmpty()) {
				Class<?> currentType = classStack.pop();

				if (!DomainResource.class.isAssignableFrom(currentType)) {
					continue;
				}

				if (cache.containsKey(currentType)) {
					continue;
				}

				final DomainResourceGraphImpl parentGraph = new DomainResourceGraphImpl<>(
						(Class<? extends DomainResource>) currentType);

				putGraph(currentType, parentGraph);
			}
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
