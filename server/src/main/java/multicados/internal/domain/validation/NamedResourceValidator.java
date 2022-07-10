/**
 * 
 */
package multicados.internal.domain.validation;

import java.io.Serializable;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Path;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.domain.AbstractGraphLogicsFactory;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.repository.GenericRepository;

class NamedResourceValidator extends AbstractDomainResourceValidator<NamedResource>
		implements AbstractGraphLogicsFactory.FixedLogic {

	private static final Logger logger = LoggerFactory.getLogger(NamedResourceValidator.class);

	private final String nameFieldName;
	private final String errorMessage;
	private final Pattern pattern;

	private final GenericRepository genericRepository;

	NamedResourceValidator(Class<? extends NamedResource> resourceType, GenericRepository genericRepository,
			String fieldName, Pattern pattern, String errorMessage) throws Exception {
		this.nameFieldName = fieldName;
		this.errorMessage = errorMessage;
		this.pattern = pattern;
		
		if (logger.isDebugEnabled()) {
			logger.debug("Compliant pattern is {} for field [{}]", pattern, fieldName);
		}

		this.genericRepository = genericRepository;
	}

	@Override
	public Validation isSatisfiedBy(EntityManager entityManager, Serializable id, NamedResource resource)
			throws Exception {
		final Validation result = Validation.success();

		if (resource.getName() == null || !pattern.matcher(resource.getName()).matches()) {
			result.bad(nameFieldName, errorMessage);
		}

		final Class<? extends NamedResource> resourceType = resource.getClass();
		final SharedSessionContractImplementor session = (SharedSessionContractImplementor) entityManager;

		if (count(resource, resourceType, session) != 0) {
			result.bad(nameFieldName, String.format("Duplicate name '%s'", resource.getName()));
		}

		return result;
	}

	private long count(NamedResource resource, final Class<? extends NamedResource> resourceType,
			final SharedSessionContractImplementor session) throws Exception {
		final EntityPersister persister = session.getFactory().unwrap(SessionFactoryImplementor.class).getMetamodel()
				.entityPersister(resourceType);
		final Serializable identifier = persister.getIdentifier(resource, session);
		// @formatter:off
		return genericRepository.count(resourceType,
				(root, cq, builder) -> {
					final Path<Object> identifierPath = root.get(persister.getIdentifierPropertyName());
					return builder.and(
							// has this name
							builder.equal(root.get(nameFieldName), resource.getName()),
							// but not this id
							identifier != null ?
									builder.notEqual(identifierPath, identifier) :
												builder.isNotNull(identifierPath));
				},
				session);
		// @formatter:on
	}

	@Override
	public Validation isSatisfiedBy(EntityManager entityManager, NamedResource resource) throws Exception {
		return isSatisfiedBy(entityManager, null, resource);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nameFieldName == null) ? 0 : nameFieldName.hashCode());
		result = prime * result + ((pattern == null) ? 0 : pattern.toString().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj == null) || (getClass() != obj.getClass()))
			return false;

		final NamedResourceValidator other = (NamedResourceValidator) obj;

		if (nameFieldName == null) {
			if (other.nameFieldName != null)
				return false;
		} else if (!nameFieldName.equals(other.nameFieldName))
			return false;
		if (pattern == null) {
			if (other.pattern != null)
				return false;
		} else if (!pattern.toString().equals(other.pattern.toString()))
			return false;
		return true;
	}

	@Override
	public String getLoggableName() {
		return String.format("%s(%s)", this.getClass().getSimpleName(), nameFieldName);
	}

}