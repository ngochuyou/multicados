/**
 * 
 */
package multicados.internal.domain.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

/**
 * @author Ngoc Huy
 *
 */
public final class AccessorFactory {

	private AccessorFactory() {}

	public static Accessor noAccess() {
		return NoOpAccessor.INSTANCE;
	}

	public static Accessor locateDirectAccessor(Class<?> ownerType, String attributeName)
			throws NoSuchFieldException, SecurityException {
		Field attribute = ownerType.getDeclaredField(attributeName);

		if (!Modifier.isPublic(attribute.getModifiers())) {
			throw new SecurityException(
					String.format("Unable to directly access to non-public attribute [%s] in type [%s]", attributeName,
							ownerType.getName()));
		}

		return new DirectAccessor(attribute);
	}

	public interface Accessor {

		Object get(Object source) throws Exception;

		void set(Object source, Object val) throws Exception;

	}

	private static abstract class AbstractAccessor implements Accessor {

		private final Getter getter;
		private final Setter setter;

		private AbstractAccessor(Getter getter, Setter setter) {
			this.getter = getter;
			this.setter = setter;
		}

		@Override
		public final Object get(Object source) throws Exception {
			return getter.get(source);
		}

		@Override
		public final void set(Object source, Object val) throws Exception {
			setter.set(source, val);
		}

		@Override
		public String toString() {
			return String.format("%s(getter=%s, setter=%s)", this.getClass().getSimpleName(), getter, setter);
		}

	}

	private static class DirectAccessor extends AbstractAccessor {

		private static final String SETTER_NAME = "DIRECT_SETTER";
		private static final String GETTER_NAME = "DIRECT_GETTER";

		private DirectAccessor(Field attribute) {
			this(new Getter() {

				@Override
				public Member getMember() {
					return attribute;
				}

				@Override
				public Class<?> getReturnedType() {
					return attribute.getType();
				}

				@Override
				public Object get(Object source) throws Exception {
					return attribute.get(source);
				}

				@Override
				public String toString() {
					return GETTER_NAME;
				}

			}, new Setter() {

				@Override
				public Member getMember() {
					return attribute;
				}

				@Override
				public void set(Object source, Object val) throws Exception {
					attribute.set(source, val);
				}

				@Override
				public String toString() {
					return SETTER_NAME;
				}
			});
		}

		private DirectAccessor(Getter getter, Setter setter) {
			super(getter, setter);
		}

	}

	private static class NoOpAccessor extends AbstractAccessor {

		private static final NoOpAccessor INSTANCE = new NoOpAccessor();

		private NoOpAccessor() {
			super(NO_OP_GETTER, OP_OP_SETTER);
		}

		private static final Getter NO_OP_GETTER = new Getter() {

			private static final String NAME = "NO_OP_GETTER";

			@Override
			public Member getMember() {
				return null;
			}

			@Override
			public Class<?> getReturnedType() {
				return null;
			}

			@Override
			public Object get(Object source) throws Exception {
				return null;
			}

			@Override
			public String toString() {
				return NAME;
			}

		};

		private static final Setter OP_OP_SETTER = new Setter() {

			private static final String NAME = "NO_OP_SETTER";

			@Override
			public Member getMember() {
				return null;
			}

			@Override
			public void set(Object source, Object val) throws Exception {}

			@Override
			public String toString() {
				return NAME;
			}

		};

	}

}
