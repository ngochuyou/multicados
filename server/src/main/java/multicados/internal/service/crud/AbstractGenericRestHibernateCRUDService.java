/**
 * 
 */
package multicados.internal.service.crud;

import java.io.Serializable;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.builder.DomainResourceBuilder;
import multicados.internal.domain.builder.DomainResourceBuilderFactory;
import multicados.internal.domain.validation.DomainResourceValidator;
import multicados.internal.domain.validation.Validation;
import multicados.internal.domain.validation.DomainResourceValidatorFactory;
import multicados.internal.service.ServiceResult;
import multicados.internal.service.crud.event.ServiceEventListenerGroups;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractGenericRestHibernateCRUDService<TUPLE> implements GenericRestHibernateCRUDService<TUPLE> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractGenericRestHibernateCRUDService.class);

	private final DomainResourceBuilderFactory builderFactory;
	private final DomainResourceValidatorFactory validatorFactory;

	private final ServiceEventListenerGroups eventListenerGroups;

	public AbstractGenericRestHibernateCRUDService(DomainResourceContext resourceContext,
			DomainResourceBuilderFactory builderFactory, DomainResourceValidatorFactory validatorFactory) throws Exception {
		this.builderFactory = builderFactory;
		this.validatorFactory = validatorFactory;
		eventListenerGroups = new ServiceEventListenerGroups(resourceContext);
	}

	@Override
	public <E extends DomainResource> ServiceResult create(Serializable id, E resource, Class<E> type,
			Session session, boolean flushOnFinish) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Creating a resource of type %s with identifier %s", type.getName(), id));
		}

		try {
			DomainResourceBuilder<E> resourceBuilder = builderFactory.getBuilder(type);

			resource = resourceBuilder.buildInsertion(id, resource, session);

			DomainResourceValidator<E> validator = validatorFactory.getValidator(type);
			Validation validation = validator.isSatisfiedBy(id, resource);

			if (!validation.isOk()) {
				return ServiceResult.bad(validation);
			}

			session.persist(resource);
			eventListenerGroups.firePostPersist(type, resource);

			return ServiceResult.success(session, flushOnFinish);
		} catch (Exception any) {
			return ServiceResult.failed(any);
		}
	}

	@Override
	public <E extends DomainResource> ServiceResult update(Serializable id, E model, Class<E> type,
			Session session, boolean flushOnFinish) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Updating a resource of type %s with identifier %s", type.getName(), id));
		}

		try {
			E persistence = session.find(type, id);

			DomainResourceBuilder<E> resourceBuilder = builderFactory.getBuilder(type);

			model = resourceBuilder.buildUpdate(id, model, persistence, session);

			DomainResourceValidator<E> validator = validatorFactory.getValidator(type);
			Validation validation = validator.isSatisfiedBy(id, model);

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
	public void summary() throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		logger.trace(eventListenerGroups.toString());
	}

}
