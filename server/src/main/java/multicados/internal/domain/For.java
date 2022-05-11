/**
 * 
 */
package multicados.internal.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * @author Ngoc Huy
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface For {

	Class<? extends DomainResource> value();

	public abstract class Message {

		public static String getMissingMessage(Class<?> type) {
			return getMissingMessage(type.getName());
		}

		public static String getMissingMessage(Field field) {
			return getMissingMessage(String.format("%s.%s", field.getDeclaringClass().getName(), field.getName()));
		}

		private static String getMissingMessage(String trailingInfo) {
			return String.format("%s is missing on %s", For.class, trailingInfo);
		}

	}

}
