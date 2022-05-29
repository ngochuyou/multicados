/**
 * 
 */
package multicados.internal.service.crud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraphCollectors;
import multicados.internal.domain.metadata.ComponentPath;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class SelectorsProvider {

	private static final Logger logger = LoggerFactory.getLogger(SelectorsProvider.class);

	private final Map<Class<? extends DomainResource>, Map<String, Function<Path<?>, Path<?>>>> selectionProducers;

	public SelectorsProvider(DomainResourceContext resourceContext) throws Exception {
		logger.debug("Resolving selectors map");

		Map<Class<? extends DomainResource>, Map<String, Function<Path<?>, Path<?>>>> selectionProducers = new HashMap<>();

		for (Class<DomainResource> type : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toTypesSet())) {
			DomainResourceMetadata<DomainResource> metadata = resourceContext.getMetadata(type);

			if (metadata == null) {
				logger.trace("Skipping type {}", type.getName());
				continue;
			}

			List<String> attributes = metadata.getAttributeNames();
			Map<String, Function<Path<?>, Path<?>>> pathProducers = new HashMap<>();

			for (String attribute : attributes) {
				if (metadata.isComponent(attribute)) {
					// @formatter:off
					pathProducers.put(attribute,
							Utils.declare(metadata)
									.second(attribute)
								.then(this::resolveComponentPathProducers)
								.get());
					continue;
					// @formatter:on
				}

				if (metadata.isAssociation(attribute)) {
					// @formatter:off
					pathProducers.put(attribute,
							metadata.isAssociationOptional(attribute)
								? (path) -> ((From<?, ?>) path).join(attribute, JoinType.LEFT)
								: (path) -> ((From<?, ?>) path).join(attribute));
					continue;
					// @formatter:on
				}

				pathProducers.put(attribute, (path) -> (Path<?>) path.get(attribute));
			}

			selectionProducers.put(type, pathProducers);
		}
		
		this.selectionProducers = Collections.unmodifiableMap(selectionProducers);
	}

	private Function<Path<?>, Path<?>> resolveComponentPathProducers(
	// @formatter:off
			DomainResourceMetadata<? extends DomainResource> metadata,
			String attributeName) { 
		List<Function<Path<?>, Path<?>>> pathNodes = individuallyResolveComponentPathProducers(
				metadata,
				attributeName,
				metadata.getComponentPaths().get(attributeName));
		// this produces a chain of functions, eventually invoke the final product (a function chain) with the root arg
		return pathNodes.stream()
				.reduce((leadingFunction, followingFunction) ->
					(currentPath) -> followingFunction.apply(leadingFunction.apply(currentPath)))
				.get();
		// @formatter:on
	}

	@SuppressWarnings("rawtypes")
	private List<Function<Path<?>, Path<?>>> individuallyResolveComponentPathProducers(
			DomainResourceMetadata<? extends DomainResource> metadata, String attributeName,
			ComponentPath componentPath) {
		Queue<String> nodeNames = componentPath.getNodeNames();

		if (metadata.isAssociation(attributeName)) {
			List<Function<Path<?>, Path<?>>> pathNodes = new ArrayList<>();
			Queue<String> copiedNodeNames = new ArrayDeque<>(nodeNames);

			pathNodes.add((metadata.isAssociationOptional(attributeName)
					? new Function<String, Function<Path<?>, Path<?>>>() {
						@Override
						public Function<Path<?>, Path<?>> apply(String nodeName) {
							return (path) -> ((From<?, ?>) path).join(nodeName, JoinType.LEFT);
						}
					}
					: new Function<String, Function<Path<?>, Path<?>>>() {
						@Override
						public Function<Path<?>, Path<?>> apply(String nodeName) {
							return (path) -> ((From<?, ?>) path).join(nodeName);
						}
					}).apply(copiedNodeNames.poll()));

			while (copiedNodeNames.size() > 1) {
				pathNodes.add(new Function<String, Function<Path<?>, Path<?>>>() {
					@Override
					public Function<Path<?>, Path<?>> apply(String name) {
						return (join) -> ((Join) join).join(name);
					}
				}.apply(copiedNodeNames.poll()));
			}

			String lastNode = copiedNodeNames.poll();
			// @formatter:off
			pathNodes.add(metadata.isAssociationOptional(lastNode)
					? (path) -> ((Join) path).join(lastNode, JoinType.LEFT)
					: (path) -> ((Join) path).join(lastNode));
			
			return pathNodes;
			// @formatter:on
		}

		return nodeNames.stream().map(nodeName -> new Function<Path<?>, Path<?>>() {
			@Override
			public Path<?> apply(Path<?> path) {
				return path.get(nodeName);
			}
		}).collect(Collectors.toList());
	}

	public <D extends DomainResource> Map<String, Function<Path<?>, Path<?>>> getSelectionProducers(Class<D> resourceType) {
		return selectionProducers.get(resourceType);
	}

}
