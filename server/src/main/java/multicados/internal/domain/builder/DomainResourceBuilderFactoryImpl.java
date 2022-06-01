/**
 * 
 */
package multicados.internal.domain.builder;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.domain.AbstractGraphWalkerFactory;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.IdentifiableResource;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.PermanentResource;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceBuilderFactoryImpl extends AbstractGraphWalkerFactory
		implements DomainResourceBuilderFactory {

	public DomainResourceBuilderFactoryImpl(DomainResourceContext resourceContext) throws Exception {
		// @formatter:off
		super(
				DomainResourceBuilder.class,
				resourceContext,
				List.of(
						Map.entry(IdentifiableResource.class, IDENTIFIABLE_RESOURCE_BUILDER),
						Map.entry(NamedResource.class, NAMED_RESOURCE_BUILDER),
						Map.entry(PermanentResource.class, PERMANENT_RESOURCE_BUILDER)),
				() -> NO_OP_BUILDER);
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends DomainResource, T extends DomainResourceBuilder<E>> DomainResourceBuilder<E> getBuilder(
			Class<E> resourceClass) {
		return (DomainResourceBuilder<E>) walkersMap.get(resourceClass);
	}

	@Override
	public void summary() throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		walkersMap.entrySet().forEach(
				entry -> logger.debug("{} -> {}", entry.getKey().getName(), entry.getValue().getLoggableName()));
	}

	private static final AbstractDomainResourceBuilder<NamedResource> NAMED_RESOURCE_BUILDER = new AbstractDomainResourceBuilder<>() {

		@Override
		public <E extends NamedResource> E buildInsertion(Serializable id, E resource, EntityManager entityManager)
				throws Exception {
			resource.setName(StringHelper.normalizeString(resource.getName()));
			return resource;
		}

		@Override
		public <E extends NamedResource> E buildUpdate(Serializable id, E model, E resource,
				EntityManager entityManger) {
			resource.setName(StringHelper.normalizeString(model.getName()));
			return resource;
		}

		@Override
		public String getLoggableName() {
			return "NamedResourceBuilder";
		}

		@Override
		public String toString() {
			return getLoggableName();
		}

	};

	private static final AbstractDomainResourceBuilder<PermanentResource> PERMANENT_RESOURCE_BUILDER = new AbstractDomainResourceBuilder<>() {
		@Override
		public <E extends PermanentResource> E buildInsertion(Serializable id, E resource, EntityManager entityManager)
				throws Exception {
			resource.setActive(Boolean.TRUE);
			return resource;
		}

		@Override
		public <E extends PermanentResource> E buildUpdate(Serializable id, E model, E resource,
				EntityManager entityManger) {
			return resource;
		}

		@Override
		public String getLoggableName() {
			return "PermanentResourceBuilder";
		}

		@Override
		public String toString() {
			return getLoggableName();
		}

	};

	@SuppressWarnings("rawtypes")
	private static final AbstractDomainResourceBuilder<IdentifiableResource> IDENTIFIABLE_RESOURCE_BUILDER = new AbstractDomainResourceBuilder<>() {

		private static final Map<Class<? extends Serializable>, Class<? extends Serializable>> TYPE_KEY_RESOLVERS = Map
				.of(String.class, String.class);

		private static final Map<Class<? extends Serializable>, Utils.HandledFunction<Serializable, Serializable, Exception>> HANDLER_RESOLVERS;

		static {
			Map<Class<? extends Serializable>, Utils.HandledFunction<Serializable, Serializable, Exception>> handlerResolvers = new HashMap<>(
					8);

			handlerResolvers.put(String.class, id -> StringHelper.normalizeString((String) id));
			handlerResolvers.put(null, id -> id);

			HANDLER_RESOLVERS = Collections.unmodifiableMap(handlerResolvers);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <E extends IdentifiableResource> E buildInsertion(Serializable id, E resource,
				EntityManager entityManager) throws Exception {
			if (id == null) {
				return resource;
			}
			// @formatter:off
			id = HANDLER_RESOLVERS
					.get(TYPE_KEY_RESOLVERS.get(id.getClass()))
					.apply(id);
			// @formatter:on
			resource.setId(id);

			return resource;
		}

		@Override
		public <E extends IdentifiableResource> E buildUpdate(Serializable id, E model, E resource,
				EntityManager entityManger) {
			return resource;
		}

		@Override
		public String getLoggableName() {
			return "IdentifiableResourceBuilder";
		}

		@Override
		public String toString() {
			return getLoggableName();
		}

	};

	private static final AbstractDomainResourceBuilder<DomainResource> NO_OP_BUILDER = new AbstractDomainResourceBuilder<DomainResource>() {

		@Override
		public <E extends DomainResource> E buildInsertion(Serializable id, E resource, EntityManager entityManager)
				throws Exception {
			return resource;
		}

		@Override
		public <E extends DomainResource> E buildUpdate(Serializable id, E model, E resource,
				EntityManager entityManger) {
			return resource;
		}

		@Override
		public String getLoggableName() {
			return "<<NO_OP>>";
		}

		@Override
		public String toString() {
			return getLoggableName();
		}

	};

}
