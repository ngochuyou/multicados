/**
 * 
 */
package multicados.internal.domain.validation;

import java.io.Serializable;

import multicados.internal.context.Loggable;
import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface Validator<T extends DomainResource> extends Loggable {

	Validation isSatisfiedBy(T resource);

	Validation isSatisfiedBy(Serializable id, T resource);

	<E extends T> Validator<E> and(Validator<E> next);

}
