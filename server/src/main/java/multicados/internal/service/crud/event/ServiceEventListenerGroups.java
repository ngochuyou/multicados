/**
 *
 */
package multicados.internal.service.crud.event;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.event.spi.PostInsertEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraphCollectors;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class ServiceEventListenerGroups {

	private static final Logger logger = LoggerFactory.getLogger(ServiceEventListenerGroups.class);

	private final Map<Class<? extends DomainResource>, List<PostPersistEventListener>> postInsertListenters;

	public ServiceEventListenerGroups(DomainResourceContext resourceContext) throws Exception {
		// @formatter:off
		this.postInsertListenters =
			Utils.declare(resourceContext.getResourceGraph())
				.then(resourceTree -> resourceTree.collect(DomainResourceGraphCollectors.toTypesSet()))
				.then(EventListenerResolver::resolvePostPersistListeners)
				.get();
		// @formatter:on
	}

	public <D extends DomainResource> void firePostPersist(Class<D> resourceType, D model) throws Exception {
		logger.debug("Firing post persist event on resource type {}", resourceType.getName());

		if (!postInsertListenters.containsKey(resourceType)) {
			return;
		}

		for (PostPersistEventListener listener : postInsertListenters.get(resourceType)) {
			listener.onPostPersist(model);
		}
	}

	@Override
	public String toString() {
		return Stream.of(String.format("%s group:\n\t%s", PostInsertEventListener.class.getSimpleName(),
				getListenerGroupLog(postInsertListenters))).collect(Collectors.joining("\n"));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getListenerGroupLog(Map listeners) {
		// @formatter:off
		return (String) listeners
			.entrySet().stream()
			.map(entry -> {
				Map.Entry<Class<DomainResource>, List<ServiceEventListener>> casted = (Map.Entry<Class<DomainResource>, List<ServiceEventListener>>) entry;

				return String.format("%s: %s", casted.getKey().getSimpleName(), casted.getValue().stream().map(ServiceEventListener::getLoggableName).collect(Collectors.joining(StringHelper.COMMON_JOINER)));
			})
			.collect(Collectors.joining("\n\t"));
		// @formatter:on
	}

}
