/**
 * 
 */
package multicados.internal.domain;

import static multicados.internal.helper.Utils.declare;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Constants;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.metadata.DomainResourceMetadataImpl;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceContextProviderImpl implements DomainResourceContextProvider {

	private final DomainResourceTree<DomainResource> resourceTree;
	@SuppressWarnings("rawtypes")
	private final DomainResourceTree<Entity> entityTree;
	private final DomainResourceTree<Model> modelTree;

	private final Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatasMap;

	public DomainResourceContextProviderImpl() throws Exception {
		// @formatter:off
		resourceTree = declare(scan())
				.then(this::buildTree)
				.identical(this::sealTree)
				.get();
		entityTree = declare(resourceTree)
				.then(root -> locateTree(root, Entity.class))
				.then(tree -> Optional.ofNullable(tree).orElseGet(() -> {
					@SuppressWarnings("rawtypes")
					DomainResourceTreeImpl<Entity> entityTree = new DomainResourceTreeImpl<>(resourceTree, Entity.class);
					
					entityTree.doAfterContextBuild();
					
					return entityTree;
				}))
				.get();
		modelTree = declare(resourceTree)
				.then(root -> locateTree(root, Model.class))
				.then(tree -> Optional.ofNullable(tree).orElseGet(() -> {
					DomainResourceTreeImpl<Model> modelTree = new DomainResourceTreeImpl<>(resourceTree, Model.class);
					
					modelTree.doAfterContextBuild();
					
					return tree;
				}))
				.get();
		metadatasMap = declare(resourceTree)
				.then(tree -> {
					List<Class<? extends DomainResource>> resourceTypes = new ArrayList<>();
					
					resourceTree.forEach(node -> resourceTypes.add(node.getResourceType()));
					
					return resourceTypes;
				})
				.then(this::buildMetadatas)
				.get();
		// @formatter:on
	}

	private Set<BeanDefinition> scan() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		final Logger logger = LoggerFactory.getLogger(DomainResourceContextProviderImpl.class);

		scanner.addIncludeFilter(new AssignableTypeFilter(DomainResource.class));

		logger.trace("Scanning for {}", DomainResource.class.getSimpleName());

		Set<BeanDefinition> candidates = scanner.findCandidateComponents(Constants.BASE_PACKAGE);

		logger.trace("Found {} candidate(s)", candidates.size());

		return candidates;
	}

	@SuppressWarnings("unchecked")
	private DomainResourceTree<DomainResource> buildTree(Set<BeanDefinition> beanDefs) throws ClassNotFoundException {
		final Logger logger = LoggerFactory.getLogger(DomainResourceContextProviderImpl.class);

		logger.trace("Building {}", DomainResourceTree.class.getSimpleName());

		DomainResourceTreeImpl<DomainResource> resourceTree = new DomainResourceTreeImpl<>(null, DomainResource.class);

		for (BeanDefinition beanDef : beanDefs) {
			Class<DomainResource> clazz = (Class<DomainResource>) Class.forName(beanDef.getBeanClassName());

			logger.trace("{} type [{}]", DomainResource.class.getSimpleName(), clazz.getName());

			Stack<?> stack = TypeHelper.getClassStack(clazz);

			while (!stack.isEmpty()) {
				resourceTree.add((Class<DomainResource>) stack.pop());
			}
		}

		return resourceTree;
	}

	private <T extends DomainResource> void sealTree(DomainResourceTree<T> tree) throws Exception {
		final Logger logger = LoggerFactory.getLogger(DomainResourceContextProviderImpl.class);

		logger.trace("Sealing {}", DomainResourceTree.class.getSimpleName());

		tree.forEach(node -> node.doAfterContextBuild());
	}

	private Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> buildMetadatas(
			List<Class<? extends DomainResource>> resourceTypes) throws Exception {
		final Logger logger = LoggerFactory.getLogger(DomainResourceContextProviderImpl.class);

		logger.trace("Building {}(s)", DomainResourceMetadata.class.getSimpleName());

		Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatasMap = new HashMap<>(
				0);
		// we use iterator for exception handling
		for (Class<? extends DomainResource> resourceType : resourceTypes) {
			if (Modifier.isInterface(resourceType.getModifiers())) {
				continue;
			}

			metadatasMap.put(resourceType, new DomainResourceMetadataImpl<>(resourceType, this));
		}

		return Collections.unmodifiableMap(metadatasMap);
	}

	@Override
	public void summary() throws Exception {
		final Logger logger = LoggerFactory.getLogger(DomainResourceContextProviderImpl.class);

		if (!logger.isDebugEnabled()) {
			return;
		}

		logger.debug("\n{}:\n\s\s\s{}", DomainResourceTree.class.getSimpleName(), visualizeTree(resourceTree, 0));
		logger.debug("\n{}", metadatasMap.values().stream().map(Object::toString).collect(Collectors.joining("\n")));
	}

	private String visualizeTree(DomainResourceTree<? extends DomainResource> node, int indentation) throws Exception {
		StringBuilder builder = new StringBuilder();
		// @formatter:off
		builder.append(String.format("%s%s\n\s\s\s",
				indentation != 0 ? String.format("%s%s",
						IntStream.range(0, indentation - 1).mapToObj(index -> "\s\s\s").collect(Collectors.joining(StringHelper.EMPTY_STRING)),
						"|__") : StringHelper.EMPTY_STRING,
				node.getResourceType().getSimpleName()));
		// @formatter:on
		if (CollectionHelper.isEmpty(node.getChildrens())) {
			return builder.toString();
		}

		node.getChildrens().forEach(children -> {
			try {
				builder.append(visualizeTree(children, indentation + 1));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return builder.toString();
	}

	@SuppressWarnings("unchecked")
	private <T extends DomainResource> DomainResourceTree<T> locateTree(DomainResourceTree<? super T> root,
			Class<T> resourceType) {
		if (root.getResourceType().equals(resourceType)) {
			return (DomainResourceTree<T>) root;
		}

		if (CollectionHelper.isEmpty(root.getChildrens())) {
			return null;
		}

		return root.getChildrens().stream().map(node -> locateTree((DomainResourceTree<? super T>) node, resourceType))
				.filter(Objects::nonNull).findFirst().orElse(null);
	}

	@Override
	public DomainResourceTree<DomainResource> getResourceTree() {
		return resourceTree;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public DomainResourceTree<Entity> getEntityTree() {
		return entityTree;
	}

	@Override
	public DomainResourceTree<Model> getModelTree() {
		return modelTree;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends DomainResource> DomainResourceMetadata<T> getMetadata(Class<T> resourceType) {
		return (DomainResourceMetadata<T>) metadatasMap.get(resourceType);
	}

}
