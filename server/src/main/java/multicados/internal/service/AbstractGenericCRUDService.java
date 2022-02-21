/**
 * 
 */
package multicados.internal.service;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.builder.DomainResourceBuilder;
import multicados.internal.domain.builder.DomainResourceBuilderFactory;
import multicados.internal.domain.validation.Validation;
import multicados.internal.domain.validation.DomainResourceValidator;
import multicados.internal.domain.validation.ValidatorFactory;
import multicados.internal.service.event.ServiceEventListenerGroups;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractGenericCRUDService<TUPLE> implements GenericCRUDService<TUPLE> {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractGenericCRUDService.class);
	
	private final DomainResourceBuilderFactory builderFactory;
	private final ValidatorFactory validatorFactory;
	
	private final ServiceEventListenerGroups eventListenerGroups;

	public AbstractGenericCRUDService(DomainResourceContext resourceContext, DomainResourceBuilderFactory builderFactory, ValidatorFactory validatorFactory)
			throws Exception {
		this.builderFactory = builderFactory;
		this.validatorFactory = validatorFactory;
		eventListenerGroups = new ServiceEventListenerGroups(resourceContext);
	}

	@Override
	public <E extends DomainResource> ServiceResult create(Serializable id, E resource, Class<E> type,
			EntityManager entityManager, boolean flushOnFinish) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Creating a resource of type %s with identifier %s", type.getName(), id));
		}
		
		try {
			DomainResourceBuilder<E> resourceBuilder = builderFactory.getBuilder(type);
			
			resource = resourceBuilder.buildInsertion(id, resource, entityManager);
			
			DomainResourceValidator<E> validator = validatorFactory.getValidator(type);
			Validation validation = validator.isSatisfiedBy(id, resource);
			
			if (!validation.isOk()) {
				return ServiceResult.bad(validation);
			}
			
			entityManager.persist(resource);
			eventListenerGroups.firePostPersist(type, resource);

			return ServiceResult.success(entityManager, flushOnFinish);
		} catch (Exception any) {
			return ServiceResult.failed(any);
		}
	}

	@Override
	public <E extends DomainResource> ServiceResult update(Serializable id, E model, Class<E> type,
			EntityManager entityManager, boolean flushOnFinish) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Updating a resource of type %s with identifier %s", type.getName(), id));
		}
		
		try {
			E persistence = entityManager.find(type, id);
			
			DomainResourceBuilder<E> resourceBuilder = builderFactory.getBuilder(type);
			
			model = resourceBuilder.buildUpdate(id, model, persistence, entityManager);
			
			DomainResourceValidator<E> validator = validatorFactory.getValidator(type);
			Validation validation = validator.isSatisfiedBy(id, model);
			
			if (!validation.isOk()) {
				return ServiceResult.bad(validation);
			}
			
			entityManager.merge(model);
			
			return ServiceResult.success(entityManager, flushOnFinish);
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
