/**
 * 
 */
package multicados.internal.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceTree;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils;
import multicados.internal.service.event.EventListenerFactory;
import multicados.internal.service.event.PostPersistEventListener;
import multicados.internal.service.event.ServiceEventListener;

/**
 * @author Ngoc Huy
 *
 */
public class GenericCRUDServiceImpl implements GenericCRUDService {

	private final Map<Class<DomainResource>, List<PostPersistEventListener<?>>> postInsertListenters;

	public GenericCRUDServiceImpl(DomainResourceContext resourceContext) throws Exception {
		// @formatter:off
		this.postInsertListenters =
			Utils.declare(resourceContext.getResourceTree())
				.then(DomainResourceTree::toSet)
				.then(EventListenerFactory::resolvePostPersistListeners)
				.get();
		// @formatter:on
	}

	@Override
	public <E extends DomainResource> ServiceResult create(Serializable id, E model, Class<E> type,
			EntityManager entityManager, boolean flushOnFinish) {
		return null;
	}

	@Override
	public void summary() throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		logger.trace("PostPersistEventListener group:\n\t{}", getListenerGroupLogging(postInsertListenters));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getListenerGroupLogging(Map listeners) {
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
