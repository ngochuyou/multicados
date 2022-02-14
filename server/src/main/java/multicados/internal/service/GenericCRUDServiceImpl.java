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
import multicados.internal.domain.validation.Validator;
import multicados.internal.domain.validation.ValidatorFactory;
import multicados.internal.service.event.ServiceEventListenerGroups;

/**
 * @author Ngoc Huy
 *
 */
public class GenericCRUDServiceImpl implements GenericCRUDService {
	
	private static final Logger logger = LoggerFactory.getLogger(GenericCRUDServiceImpl.class);
	
	private final DomainResourceBuilderFactory builderFactory;
	private final ValidatorFactory validatorFactory;
	
	private final ServiceEventListenerGroups eventListenerGroups;

	public GenericCRUDServiceImpl(DomainResourceContext resourceContext, DomainResourceBuilderFactory builderFactory, ValidatorFactory validatorFactory)
			throws Exception {
		this.builderFactory = builderFactory;
		this.validatorFactory = validatorFactory;
		eventListenerGroups = new ServiceEventListenerGroups(resourceContext);
	}

	@Override
	public <E extends DomainResource> ServiceResult create(Serializable id, E model, Class<E> type,
			EntityManager entityManager, boolean flushOnFinish) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Creating a resource of type %s with identifier %s", type.getName(), id));
		}
		
		try {
			DomainResourceBuilder<E> resourceBuilder = builderFactory.getBuilder(type);
			
			model = resourceBuilder.buildInsertion(id, model, entityManager);
			
			Validator<E> validator = validatorFactory.getValidator(type);
			Validation validation = validator.isSatisfiedBy(id, model);
			
			if (!validation.isOk()) {
				return ServiceResult.bad(validation);
			}
			
			entityManager.persist(model);
			eventListenerGroups.firePostPersist(type, model);

			return null;
		} catch (Exception any) {
			any.printStackTrace();
			return ServiceResult.failed(any);
		}
	}

	@Override
	public void summary() throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		logger.trace(eventListenerGroups.toString());
	}

}
