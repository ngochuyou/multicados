/**
 *
 */
package multicados.internal.service.crud;

import java.io.Serializable;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.builder.DomainResourceBuilder;
import multicados.internal.domain.builder.DomainResourceBuilderFactory;
import multicados.internal.domain.validation.DomainResourceValidator;
import multicados.internal.domain.validation.DomainResourceValidatorFactory;
import multicados.internal.domain.validation.Validation;
import multicados.internal.service.ServiceResult;
import multicados.internal.service.crud.event.ServiceEventListenerGroups;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractGenericHibernateCUDService<TUPLE> extends ContextBuilder.AbstractContextBuilder
		implements GenericCRUDService<TUPLE, Session> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractGenericHibernateCUDService.class);

	private final DomainResourceBuilderFactory builderFactory;
	private final DomainResourceValidatorFactory validatorFactory;

	private final ServiceEventListenerGroups eventListenerGroups;

	public AbstractGenericHibernateCUDService(DomainResourceContext resourceContext,
			DomainResourceBuilderFactory builderFactory, DomainResourceValidatorFactory validatorFactory)
			throws Exception {
		this.builderFactory = builderFactory;
		this.validatorFactory = validatorFactory;
		eventListenerGroups = new ServiceEventListenerGroups(resourceContext);
	}

	@Override
	public <T extends DomainResource> ServiceResult create(Serializable id, T resource, Class<T> type, Session session,
			boolean flushOnFinish) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Creating a resource of type %s with identifier %s", type.getName(), id));
		}

		try {
			final DomainResourceBuilder<T> resourceBuilder = builderFactory.getBuilder(type);

			resource = resourceBuilder.buildInsertion(id, resource, session);

			final DomainResourceValidator<T> validator = validatorFactory.getValidator(type);
			final Validation validation = validator.isSatisfiedBy(session, id, resource);

			if (!validation.isOk()) {
				return ServiceResult.bad(validation);
			}

			session.save(resource);
			eventListenerGroups.firePostPersist(type, resource);

			return ServiceResult.success(session, flushOnFinish);
		} catch (Exception any) {
			return ServiceResult.failed(any);
		}
	}

	@Override
	public <T extends DomainResource> ServiceResult update(Serializable id, T model, Class<T> type, Session session,
			boolean flushOnFinish) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Updating a resource of type %s with identifier %s", type.getName(), id));
		}

		try {
			final T persistence = session.find(type, id);

			final DomainResourceBuilder<T> resourceBuilder = builderFactory.getBuilder(type);

			model = resourceBuilder.buildUpdate(id, model, persistence, session);

			final DomainResourceValidator<T> validator = validatorFactory.getValidator(type);
			final Validation validation = validator.isSatisfiedBy(session, id, model);

			if (!validation.isOk()) {
				return ServiceResult.bad(validation);
			}

			session.merge(model);

			return ServiceResult.success(session, flushOnFinish);
		} catch (Exception any) {
			return ServiceResult.failed(any);
		}
	}

	@Override
	public void summary() {
		logger.trace(eventListenerGroups.toString());
	}

}
