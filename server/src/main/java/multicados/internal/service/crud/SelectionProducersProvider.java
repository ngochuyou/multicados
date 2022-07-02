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
import multicados.internal.domain.metadata.DomainResourceAttributesMetadata;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class SelectionProducersProvider {

	private static final Logger logger = LoggerFactory.getLogger(SelectionProducersProvider.class);

	private final Map<Class<? extends DomainResource>, Map<String, Function<Path<?>, Path<?>>>> selectionProducers;

	@SuppressWarnings("unchecked")
	public SelectionProducersProvider(DomainResourceContext resourceContext) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Resolving selection producers map");
		}

		final Map<Class<? extends DomainResource>, Map<String, Function<Path<?>, Path<?>>>> selectionProducers = new HashMap<>();

		for (final Class<? extends DomainResource> type : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toTypesSet())) {
			final DomainResourceAttributesMetadata<? extends DomainResource> metadata = resourceContext
					.getMetadata(type).unwrap(DomainResourceAttributesMetadata.class);

			if (metadata == null) {
				continue;
			}

			final List<String> attributes = metadata.getAttributeNames();
			final Map<String, Function<Path<?>, Path<?>>> pathProducers = new HashMap<>();

			for (final String attribute : attributes) {
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
			DomainResourceAttributesMetadata<? extends DomainResource> metadata,
			String attributeName) {
		final List<Function<Path<?>, Path<?>>> pathNodes = individuallyResolveComponentPathProducers(
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
			DomainResourceAttributesMetadata<? extends DomainResource> metadata, String attributeName,
			ComponentPath componentPath) {
		final Queue<String> nodeNames = componentPath.getPath();

		if (metadata.isAssociation(attributeName)) {
			final List<Function<Path<?>, Path<?>>> pathNodes = new ArrayList<>();
			final Queue<String> copiedNodeNames = new ArrayDeque<>(nodeNames);

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

			final String lastNode = copiedNodeNames.poll();
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

	public <D extends DomainResource> Map<String, Function<Path<?>, Path<?>>> getSelectionProducers(
			Class<D> resourceType) {
		return selectionProducers.get(resourceType);
	}

}
