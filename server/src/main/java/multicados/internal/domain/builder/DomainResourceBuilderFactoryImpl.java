/**
 *
 */
package multicados.internal.domain.builder;

import static java.util.Map.entry;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import multicados.internal.domain.AbstractGraphLogicsFactory;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.GraphLogic;
import multicados.internal.domain.IdentifiableResource;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.PermanentResource;
import multicados.internal.domain.metadata.IdentifiableResourceMetadata;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceBuilderFactoryImpl extends AbstractGraphLogicsFactory
		implements DomainResourceBuilderFactory {

	private static final Logger logger = LoggerFactory.getLogger(DomainResourceBuilderFactoryImpl.class);

	@Autowired
	public DomainResourceBuilderFactoryImpl(ApplicationContext applicationContext,
			DomainResourceContext resourceContext) throws Exception {
		// @formatter:off
		super(
				applicationContext,
				DomainResourceBuilder.class,
				resourceContext,
				() -> NO_OP_BUILDER);
		// @formatter:on
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Collection<Entry<Class, Entry<Class, GraphLogic>>> getFixedLogics(ApplicationContext applicationContext) {
		// @formatter:off
		return List.of(
				entry(IdentifiableResource.class, entry(IdentifiableResource.class, new IdentifiableResourceBuilder(applicationContext.getBean(DomainResourceContext.class)))),
				entry(NamedResource.class, entry(NamedResource.class, NAMED_RESOURCE_BUILDER)),
				entry(PermanentResource.class, entry(PermanentResource.class, PERMANENT_RESOURCE_BUILDER)));
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends DomainResource, T extends DomainResourceBuilder<E>> DomainResourceBuilder<E> getBuilder(
			Class<E> resourceClass) {
		return (DomainResourceBuilder<E>) logicsMap.get(resourceClass);
	}

	@Override
	public void summary() {
		for (@SuppressWarnings("rawtypes")
		final Entry<Class, GraphLogic> entry : logicsMap.entrySet()) {
			logger.debug("Using {} for {}", entry.getValue().getLoggableName(), entry.getKey().getSimpleName());
		}
	}

	private static final AbstractDomainResourceBuilder<NamedResource> NAMED_RESOURCE_BUILDER = new AbstractDomainResourceBuilder<>() {

		@Override
		public NamedResource buildInsertion(NamedResource resource, EntityManager entityManager) throws Exception {
			resource.setName(StringHelper.normalizeString(resource.getName()));
			return resource;
		}

		@Override
		public NamedResource buildUpdate(NamedResource model, NamedResource resource, EntityManager entityManger) {
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
		public PermanentResource buildInsertion(PermanentResource persistence, EntityManager entityManager)
				throws Exception {
			persistence.setActive(Boolean.TRUE);
			return persistence;
		}

		@Override
		public PermanentResource buildUpdate(PermanentResource model, PermanentResource persistence,
				EntityManager entityManger) {
			return persistence;
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
	private static class IdentifiableResourceBuilder extends AbstractDomainResourceBuilder<IdentifiableResource<?>>
			implements FixedLogic {

		private static final Logger logger = LoggerFactory.getLogger(DomainResourceBuilderFactoryImpl.class);

		private final DomainResourceContext resourceContext;

		public IdentifiableResourceBuilder(DomainResourceContext resourceContext) {
			this.resourceContext = resourceContext;
		}

		@SuppressWarnings("unchecked")
		private IdentifiableResource doMandatory(IdentifiableResource persistence) {
			final Class<? extends IdentifiableResource> resourceType = persistence.getClass();

			if (resourceContext.getMetadata(resourceType).unwrap(IdentifiableResourceMetadata.class)
					.isIdentifierAutoGenerated()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Stripping identifier off resource type {}", resourceType.getName());
				}

				persistence.setId(null);
			}

			return persistence;
		}

		@Override
		public IdentifiableResource buildInsertion(IdentifiableResource persistence, EntityManager entityManager)
				throws Exception {
			return doMandatory(persistence);
		}

		@Override
		public IdentifiableResource buildUpdate(IdentifiableResource model, IdentifiableResource persistence,
				EntityManager entityManger) {
			return persistence;
		}

		@Override
		public String getLoggableName() {
			return "IdentifiableResourceBuilder";
		}

		@Override
		public String toString() {
			return getLoggableName();
		}

	}

	private static final AbstractDomainResourceBuilder<DomainResource> NO_OP_BUILDER = new AbstractDomainResourceBuilder<>() {

		@Override
		public DomainResource buildInsertion(DomainResource resource, EntityManager entityManager) throws Exception {
			return resource;
		}

		@Override
		public DomainResource buildUpdate(DomainResource model, DomainResource resource, EntityManager entityManger) {
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
