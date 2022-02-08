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
public interface ValidatorFactory extends ContextBuilder {

	<T extends DomainResource> Validator<T> getValidator(Class<T> resourceType);

}