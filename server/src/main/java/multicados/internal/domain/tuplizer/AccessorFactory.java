/**
 * 
 */
package multicados.internal.domain.tuplizer;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import multicados.internal.helper.FunctionHelper.HandledBiFunction;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
public final class AccessorFactory {

	private AccessorFactory() {}

	public static Accessor noop() {
		return NoOpAccessor.INSTANCE;
	}

	public static Accessor direct(Class<?> ownerType, String propName) {
		return new DirectAccessor(propName, ownerType);
	}

	public static Accessor standard(Class<?> ownerType, String propName)
			throws NoSuchFieldException, SecurityException {
		return new StandardAccessor(propName, ownerType);
	}

	public static Accessor delegate(Getter getter, Setter setter) {
		return new AbstractAccessor(getter, setter) {};
	}

	public interface Accessor {

		Object get(Object source) throws Exception;

		void set(Object source, Object val) throws Exception;

		Getter getGetter();

		Setter getSetter();

	}

	static abstract class AbstractAccessor implements Accessor {

		private final Getter getter;
		private final Setter setter;

		AbstractAccessor(Getter getter, Setter setter) {
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

		@Override
		public Getter getGetter() {
			return getter;
		}

		@Override
		public Setter getSetter() {
			return setter;
		}

	}

	public static class StandardAccessor extends AbstractAccessor {

		public static <T> Getter locateGetter(String propName, Class<T> owningType) {
			try {
				owningType.getDeclaredField(propName);

				return new Getter() {

					private final Method getterMethod = owningType
							.getDeclaredMethod(StringHelper.combineIntoCamel("get", propName));

					@Override
					public Member getMember() {
						return getterMethod;
					}

					@Override
					public Class<?> getReturnedType() {
						return getterMethod.getReturnType();
					}

					@Override
					public Object get(Object source) throws Exception {
						return getterMethod.invoke(source);
					}

					@Override
					public String toString() {
						return String.format("%s()", getterMethod.getName());
					}

				};
			} catch (NoSuchFieldException | SecurityException | NoSuchMethodException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(String.format("Unable to locate %s for field name [%s]",
						Getter.class.getSimpleName(), propName));
			}
		}

		private static <T> Setter locateSetter(String propName, Class<T> owningType) {
			try {
				Field declaredField;

				try {
					declaredField = owningType.getDeclaredField(propName);

					HandledBiFunction<String, Class<?>, Method, Exception> setterMethodProducer = (prop, owner) -> {
						return owner.getDeclaredMethod(StringHelper.combineIntoCamel("set", prop),
								declaredField.getType());
					};

					return new AbstractSetter(propName, owningType, setterMethodProducer) {

						private final Method setterMethod = setterMethodProducer.apply(propName, owningType);

						@Override
						public Member getMember() {
							return setterMethod;
						}

						@Override
						public String toString() {
							return String.format("%s(%s)", setterMethod.getName(),
									setterMethod.getReturnType().getName());
						}

					};
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
					throw new IllegalArgumentException(
							String.format("Unable to locate field member with name [%s]", propName));
				}
			} catch (SecurityException | NoSuchMethodException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(String.format("Unable to locate %s for field name [%s]",
						Setter.class.getSimpleName(), propName));
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}

		private <T> StandardAccessor(String propName, Class<T> owningType) {
			super(locateGetter(propName, owningType), locateSetter(propName, owningType));
		}

	}

	public static class DirectAccessor extends AbstractAccessor {

		private static Field checkAccess(String propName, Class<?> owningType) {
			Field field;

			try {
				field = owningType.getDeclaredField(propName);
			} catch (NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
				throw new SecurityException(String.format("Unable to locate field member with name [%s]", propName));
			}

			if (!Modifier.isPublic(field.getModifiers())) {
				throw new SecurityException(
						String.format("Unable to directly access to non-public attribute [%s] in type [%s]", propName,
								owningType.getName()));
			}

			return field;
		}

		public static <T> Getter locateGetter(String propName, Class<T> owningType) {
			return new Getter() {

				private final Field field = checkAccess(propName, owningType);

				@Override
				public Member getMember() {
					return field;
				}

				@Override
				public Class<?> getReturnedType() {
					return field.getType();
				}

				@Override
				public Object get(Object source) throws Exception {
					return field.get(source);
				}

				@Override
				public String toString() {
					return String.format("%s", field.getName());
				}

			};
		}

		public static <T> Setter locateSetter(String propName, Class<T> owningType) {
			try {
				return new AbstractSetter(propName, owningType) {

					private final Field field = checkAccess(propName, owningType);

					@Override
					public Member getMember() {
						return field;
					}

					@Override
					public String toString() {
						return String.format("%s(%s)", field.getName(), field.getType().getName());
					}

				};
			} catch (NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
				throw new SecurityException(String.format("Unable to locate field member with name [%s]", propName));
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}

		private DirectAccessor(String propName, Class<?> owningType) {
			super(locateGetter(propName, owningType), locateSetter(propName, owningType));
		}

	}

	private static class NoOpAccessor extends AbstractAccessor {

		private static final NoOpAccessor INSTANCE = new NoOpAccessor();

		private NoOpAccessor() {
			super(NO_OP_GETTER, OP_OP_SETTER);
		}

		public static final Getter NO_OP_GETTER = new Getter() {

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

		public static final Setter OP_OP_SETTER = new Setter() {

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
