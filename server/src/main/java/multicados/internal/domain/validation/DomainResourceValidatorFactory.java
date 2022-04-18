/**
 * 
 */
package multicados.internal.domain.validation;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceValidatorFactory extends ContextBuilder {

	<T extends DomainResource> DomainResourceValidator<T> getValidator(Class<T> resourceType);

}
