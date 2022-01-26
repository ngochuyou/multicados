/**
 * 
 */
package multicados.internal.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Ngoc Huy
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface For {

	Class<? extends DomainResource> value();

	public static final String MISSING_MESSAGE = String.format("%s is missing", For.class);

}
