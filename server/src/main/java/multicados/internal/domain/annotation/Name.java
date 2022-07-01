/**
 * 
 */
package multicados.internal.domain.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import multicados.internal.domain.NamedResource;

/**
 * Indicates the targeted field fully inherits every logic of the
 * {@link NamedResource}
 * 
 * @author Ngoc Huy
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Name {

	boolean useDefault() default true;

	public static interface Message {

		public static String getMissingMessage(Class<?> resourceType) {
			return String.format("@%s is missing on type %s", Name.class.getSimpleName(), resourceType.getName());
		}

	}

}
