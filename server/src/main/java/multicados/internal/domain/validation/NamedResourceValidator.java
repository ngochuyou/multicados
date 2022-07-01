/**
 * 
 */
package multicados.internal.domain.validation;

import java.io.Serializable;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.domain.AbstractGraphLogicsFactory;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.repository.GenericRepository;

class NamedResourceValidator extends AbstractDomainResourceValidator<NamedResource>
		implements AbstractGraphLogicsFactory.FixedLogic {

	private static final Logger logger = LoggerFactory.getLogger(NamedResourceValidator.class);

	private final String fieldName;
	private final String errorMessage;
	private final Pattern pattern;

	private final GenericRepository genericRepository;

	NamedResourceValidator(GenericRepository genericRepository, String fieldName, Pattern pattern, String errorMessage)
			throws Exception {
		this.fieldName = fieldName;
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
			result.bad(fieldName, errorMessage);
		}

		final Class<? extends NamedResource> resourceType = resource.getClass();
		final Session session = (Session) entityManager;

		if (genericRepository.count(resourceType,
				(root, cq, builder) -> builder.equal(root.get(fieldName), resource.getName()), session) != 0) {
			result.bad(fieldName, String.format("Duplicate name '%s'", resource.getName()));
		}

		return result;
	}

	@Override
	public Validation isSatisfiedBy(EntityManager entityManager, NamedResource resource) throws Exception {
		return isSatisfiedBy(entityManager, null, resource);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
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

		if (fieldName == null) {
			if (other.fieldName != null)
				return false;
		} else if (!fieldName.equals(other.fieldName))
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
		return String.format("%s(%s)", this.getClass().getSimpleName(), fieldName);
	}

}