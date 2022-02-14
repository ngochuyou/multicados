/**
 * 
 */
package multicados.internal.domain.validation;

import java.io.Serializable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.domain.AbstractGraphWalkerFactory;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;

/**
 * @author Ngoc Huy
 *
 */
public class ValidatorFactoryImpl extends AbstractGraphWalkerFactory implements ValidatorFactory {

	public ValidatorFactoryImpl(DomainResourceContext resourceContextProvider) throws Exception {
		super(Validator.class, resourceContextProvider, List.of(), () -> NO_OP_VALIDATOR);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends DomainResource> Validator<T> getValidator(Class<T> resourceType) {
		return (Validator<T>) walkersMap.get(resourceType);
	}

	@Override
	public void summary() throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		walkersMap.entrySet().forEach(
				entry -> logger.debug("{} -> {}", entry.getKey().getName(), entry.getValue().getLoggableName()));
	}

	@SuppressWarnings("rawtypes")
	private static final Validator NO_OP_VALIDATOR = new Validator() {

		@Override
		public Validation isSatisfiedBy(DomainResource resource) {
			return Validation.success();
		}

		@Override
		public Validation isSatisfiedBy(Serializable id, DomainResource resource) {
			return Validation.success();
		}

		@Override
		public Validator and(Validator next) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getLoggableName() {
			return "<<NO_OP>>";
		}

	};

}
