/**
 * 
 */
package multicados.internal.service.event;

import static multicados.internal.helper.Utils.declare;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import multicados.internal.context.ContextBuildListener;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.EncryptedIdentifierResource;
import multicados.internal.helper.Base32;
import multicados.internal.helper.FunctionHelper.HandledFunction;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class EventListenerFactory implements ContextBuildListener {

	private static EventListenerFactory INSTANCE = new EventListenerFactory();

	private EventListenerFactory() {}

	@Override
	public void doAfterContextBuild() {
		INSTANCE = null;
	}

	@SuppressWarnings("unchecked")
	private List<Class<DomainResource>> wrap(Collection<Class<? extends DomainResource>> unwrappedTypes) {
		return unwrappedTypes.stream().map(unwrappedType -> (Class<DomainResource>) unwrappedType)
				.collect(Collectors.toList());
	}

	private Map<Class<DomainResource>, List<PostPersistEventListener<?>>> doResolvePostPersistListeners(
			Collection<Class<DomainResource>> resourceTypes) throws Exception {
		// @formatter:off
		return declare(resourceTypes)
				.then(this::registerFixedPostPersistListeners)
					.second(resourceTypes)
				.then(this::registerContributedPostPersistListeners)
				.get();
		// @formatter:on
	}

	private Map<Class<DomainResource>, List<PostPersistEventListener<?>>> registerContributedPostPersistListeners(
			Map<Class<DomainResource>, List<PostPersistEventListener<?>>> listeners,
			Collection<Class<DomainResource>> resourceTypes) {
		return listeners;
	}

	private Map<Class<DomainResource>, List<PostPersistEventListener<?>>> registerFixedPostPersistListeners(
			Collection<Class<DomainResource>> resourceTypes) {
		Map<Class<DomainResource>, List<PostPersistEventListener<?>>> listeners = new HashMap<>();
		// @formatter:off
		final Map<Class<? extends DomainResource>, HandledFunction<Class<DomainResource>, List<PostPersistEventListener<?>>, Exception>> listenersResolvers = Map.of(
				EncryptedIdentifierResource.class,
						type -> List.of(new EncryptedIdentifierResourcePostPersistEventListener<>()));
		// @formatter:on
		for (Class<DomainResource> type : resourceTypes) {
			for (Class<?> interfaceType : ClassUtils.getAllInterfacesForClassAsSet(type)) {
				if (listenersResolvers.containsKey(interfaceType)) {
					listeners.compute(type, (key, currentListeners) -> {
						List<PostPersistEventListener<?>> nextListeners;

						try {
							nextListeners = listenersResolvers.get(interfaceType).apply(type);
						} catch (Exception e) {
							e.printStackTrace();
							return null;
						}

						if (currentListeners == null) {
							return nextListeners;
						}

						return Stream.of(currentListeners, nextListeners).flatMap(List::stream)
								.collect(Collectors.toList());
					});
				}
			}
		}

		return listeners;
	}

	public static Map<Class<DomainResource>, List<PostPersistEventListener<?>>> resolvePostPersistListeners(
			Set<Class<? extends DomainResource>> resourceTypes) throws Exception {
		// @formatter:off
		return Utils.declare(INSTANCE.wrap(resourceTypes))
				.then(INSTANCE::doResolvePostPersistListeners)
				.get();
		// @formatter:on
	}

	private static class EncryptedIdentifierResourcePostPersistEventListener<E extends EncryptedIdentifierResource<BigInteger>>
			implements PostPersistEventListener<E> {

		private static final Logger logger = LoggerFactory
				.getLogger(EventListenerFactory.EncryptedIdentifierResourcePostPersistEventListener.class);
		private static final String LOG_TEMPLATE = "Generated code {} from identifier {}";

		@Override
		public void onPostInsert(E resource) throws Exception {
			String code = Base32.crockfords.format(resource.getId());

			resource.setCode(code);

			if (logger.isTraceEnabled()) {
				logger.trace(LOG_TEMPLATE, code, resource.getId());
			}
		}

		@Override
		public String getLoggableName() {
			return "EncryptedIdentifierResourcePostPersistEventListener";
		}

	}

}
