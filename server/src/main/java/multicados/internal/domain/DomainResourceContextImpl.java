/**
 * 
 */
package multicados.internal.domain;

import static multicados.internal.helper.Utils.declare;

import java.lang.reflect.Modifier;
import java.util.Collection;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.metadata.DomainResourceMetadataImpl;
import multicados.internal.domain.tuplizer.DomainEntityTuplizer;
import multicados.internal.domain.tuplizer.DomainResourceTuplizer;
import multicados.internal.file.domain.FileResource;
import multicados.internal.file.domain.Image;
import multicados.internal.file.engine.FileManagement;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils.HandledFunction;

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
			logger.trace("Instantiating {}", DomainResourceContextImpl.class.getName());
		}

		final SessionFactoryImplementor sfi = sessionFactory.unwrap(SessionFactoryImplementor.class);
		// @formatter:off
		resourceGraph = declare(scan())
				.then(this::buildGraph)
				.consume(this::sealGraph)
				.get();
		metadatasMap = declare(resourceGraph, sfi, fileManagement)
					.map(resourceGraph -> resourceGraph.collect(DomainResourceGraphCollectors.toTypesSet()), HandledFunction.identity(), HandledFunction.identity())
				.then(this::buildMetadatas)
				.then(Collections::unmodifiableMap)
				.get();
		tuplizersMap = declare(resourceGraph, sfi)
					.map(resourceGraph -> resourceGraph.collect(DomainResourceGraphCollectors.toTypesSet()), HandledFunction.identity())
				.then(this::buildTuplizers)
				.then(Collections::unmodifiableMap)
				.get();
		// @formatter:on
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Class<? extends DomainResource>, DomainResourceTuplizer<? extends DomainResource>> buildTuplizers(
			Collection<Class<DomainResource>> entityTypes, SessionFactoryImplementor sfi) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Building {}", DomainResourceTuplizer.class.getSimpleName());
		}

		final Map<Class<? extends DomainResource>, DomainResourceTuplizer<? extends DomainResource>> tuplizers = new HashMap<>();

		for (Class type : entityTypes) {
			if (tuplizers.containsKey(type)) {
				continue;
			}

			if (Entity.class.isAssignableFrom(type) && !Modifier.isAbstract(type.getModifiers())) {
				tuplizers.put(type, new DomainEntityTuplizer<>(type, getMetadata(type), sfi));
				continue;
			}
		}

		return tuplizers;
	}

	private Set<BeanDefinition> scan() {
		final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
				false);

		if (logger.isTraceEnabled()) {
			logger.trace("Scanning for {}", DomainResource.class.getSimpleName());
		}

		scanner.addIncludeFilter(new AssignableTypeFilter(DomainResource.class));

		final Set<BeanDefinition> candidates = scanner.findCandidateComponents(Settings.BASE_PACKAGE);

		if (logger.isDebugEnabled()) {
			logger.debug("Found {} {}", candidates.size(), DomainResource.class.getName());
		}

		return candidates;
	}

	@SuppressWarnings("unchecked")
	private DomainResourceGraph<DomainResource> buildGraph(Set<BeanDefinition> beanDefs) throws ClassNotFoundException {
		if (logger.isTraceEnabled()) {
			logger.trace("Building {}", DomainResourceGraph.class.getSimpleName());
		}

		final DomainResourceGraphImpl<DomainResource> resourceGraph = constructRootNodes();

		for (final BeanDefinition beanDef : beanDefs) {
			final Class<DomainResource> clazz = (Class<DomainResource>) Class.forName(beanDef.getBeanClassName());
			final Stack<?> stack = TypeHelper.getClassStack(clazz);

			while (!stack.isEmpty()) {
				resourceGraph.add((Class<DomainResource>) stack.pop());
			}
		}

		return resourceGraph;
	}

	private DomainResourceGraphImpl<DomainResource> constructRootNodes() {
		final DomainResourceGraphImpl<DomainResource> root = new DomainResourceGraphImpl<>(DomainResource.class);
		final DomainResourceGraph<? extends DomainResource> identifiable = root.add(IdentifiableResource.class);

		root.add(NamedResource.class);
		root.add(PermanentResource.class);
		root.add(SpannedResource.class);
		root.add(AuditableResource.class);

		identifiable.add(EncryptedIdentifierResource.class);
		identifiable.add(Entity.class);
		identifiable.add(FileResource.class).add(Image.class);

		return root;
	}

	private <T extends DomainResource> void sealGraph(DomainResourceGraph<T> graph) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Sealing {}", DomainResourceGraph.class.getSimpleName());
		}

		graph.forEach(node -> node.doAfterContextBuild());
	}

	private Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> buildMetadatas(
			Collection<Class<DomainResource>> resourceTypes, SessionFactoryImplementor sfi,
			FileManagement fileManagement) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Building {}(s)", DomainResourceMetadata.class.getSimpleName());
		}

		final Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatasMap = new HashMap<>(
				0);

		for (Class<? extends DomainResource> resourceType : resourceTypes) {
			if (metadatasMap.containsKey(resourceType) || Modifier.isInterface(resourceType.getModifiers())) {
				continue;
			}

			DomainResourceMetadataImpl<? extends DomainResource> metadata = new DomainResourceMetadataImpl<>(
					resourceType, this, metadatasMap, sfi, fileManagement);

			metadatasMap.put(resourceType, metadata);
		}

		return metadatasMap;
	}

	@Override
	public void summary() {
		if (!logger.isDebugEnabled()) {
			return;
		}

		logger.info("\n{}:\n\s\s\s{}", DomainResourceGraph.class.getSimpleName(), visualizeGraph(resourceGraph, 0));
		logger.info("\n{}", metadatasMap.values().stream().map(Object::toString).collect(Collectors.joining("\n")));
	}

	private String visualizeGraph(DomainResourceGraph<? extends DomainResource> node, int indentation) {
		final StringBuilder builder = new StringBuilder();
		// @formatter:off
		builder.append(String.format("%s%s[%d]\n\s\s\s",
				indentation != 0 ? String.format("%s%s",
						IntStream.range(0, indentation).mapToObj(index -> "\s\s\s").collect(Collectors.joining(StringHelper.EMPTY_STRING)),
						"|__") : StringHelper.EMPTY_STRING,
				node.getResourceType().getSimpleName(), node.getDepth()));
		// @formatter:on
		if (CollectionHelper.isEmpty(node.getChildrens())) {
			return builder.toString();
		}

		for (final DomainResourceGraph<?> children : node.getChildrens()) {
			builder.append(visualizeGraph(children, indentation + 1));
		}

		return builder.toString();
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

}
