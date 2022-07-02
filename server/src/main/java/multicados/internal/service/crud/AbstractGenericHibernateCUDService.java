/**
 *
 */
package multicados.internal.service.crud;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.IdentifiableResource;
import multicados.internal.domain.builder.DomainResourceBuilder;
import multicados.internal.domain.builder.DomainResourceBuilderFactory;
import multicados.internal.domain.validation.DomainResourceValidator;
import multicados.internal.domain.validation.DomainResourceValidatorFactory;
import multicados.internal.domain.validation.Validation;
import multicados.internal.helper.Common;
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
	public <S extends Serializable, T extends IdentifiableResource<S>> ServiceResult create(Serializable id, T model,
			Class<T> type, Session session, boolean flushOnFinish) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Creating a resource of type %s with identifier %s", type.getName(), id));
		}

		try {
			final DomainResourceBuilder<T> resourceBuilder = builderFactory.getBuilder(type);

			resourceBuilder.buildInsertion(model, session);

			final DomainResourceValidator<T> validator = validatorFactory.getValidator(type);
			final Validation validation = validator.isSatisfiedBy(session, id, model);

			if (!validation.isOk()) {
				return ServiceResult.bad(validation);
			}

			session.save(model);
			eventListenerGroups.firePostPersist(type, model);

			return ServiceResult.success(session, flushOnFinish);
		} catch (Exception any) {
			// if the exception is indirectly PersistenceException, wrap it in
			// a PersistenceException so that we handle it in via ExceptionAdvisor
			if (any instanceof PersistenceException) {
				if (!any.getClass().equals(PersistenceException.class)) {
					return ServiceResult.failed(new PersistenceException(any));
				}
			}

			return ServiceResult.failed(any);
		}
	}

	private void throwNotFound(Serializable id) {
		throw new PersistenceException(
				new EntityNotFoundException(id == null ? Common.notFound() : Common.notFound(List.of(id.toString()))));
	}

	@Override
	public <S extends Serializable, T extends IdentifiableResource<S>> ServiceResult update(Serializable id, T model,
			Class<T> type, Session session, boolean flushOnFinish) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Updating a resource of type %s with identifier %s", type.getName(), id));
		}

		try {
			final Serializable actualId = Optional.ofNullable(id).orElse(model.getId());
			final T persistence = session.find(type, actualId);

			if (persistence == null) {
				throwNotFound(actualId);
			}

			final DomainResourceBuilder<T> resourceBuilder = builderFactory.getBuilder(type);

			resourceBuilder.buildUpdate(model, persistence, session);

			final DomainResourceValidator<T> validator = validatorFactory.getValidator(type);
			final Validation validation = validator.isSatisfiedBy(session, actualId, model);

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
		if (logger.isDebugEnabled()) {
			logger.debug(eventListenerGroups.toString());
		}
	}

}
