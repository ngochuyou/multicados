/**
 *
 */
package multicados.internal.domain.metadata;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.metadata.DomainResourceAttributesMetadataImpl.DomainAssociation.OptionalAssociation;

/**
 * Fully describes a {@link DomainResource}
 * 
 * The root role of this implementation is the
 * {@link DomainResourceAttributesMetadata}, any other metadata will be
 * registered as a dependency
 * 
 * @author Ngoc Huy
 *
 */
public class DomainResourceAttributesMetadataImpl<T extends DomainResource> extends AbstractDomainResourceMetadata<T>
		implements DomainResourceAttributesMetadata<T> {

	private final List<String> declaredAttributeNames;
	private final List<String> unwrappedAttributeNames;
	private final List<String> wrappedAttributeNames;
	private final List<String> nonLazyAttributeNames;
	private final Map<String, Class<?>> attributeTypes;

	private final Map<String, ComponentPath> componentPaths;
	private final Map<String, DomainAssociation> associations;

	private final Map<Class<? extends DomainResourceMetadata<T>>, DomainResourceMetadata<T>> additionalMetadatas;

	public DomainResourceAttributesMetadataImpl(
	// @formatter:off
			Class<T> resourceType,
			List<String> declaredAttributeNames,
			List<String> wrappedAttributeNames,
			List<String> unwrappedAttributeNames,
			Map<String, Class<?>> attributeTypes,
			List<String> nonLazyAttributeNames,
			Map<String, ComponentPath> componentPaths,
			Map<String, DomainAssociation> associations,
			Map<Class<? extends DomainResourceMetadata<T>>, DomainResourceMetadata<T>> additionalMetadatas) {
		// @formatter:on
		super(resourceType);
		this.declaredAttributeNames = unmodifiableList(declaredAttributeNames);
		this.unwrappedAttributeNames = unmodifiableList(unwrappedAttributeNames);
		this.wrappedAttributeNames = unmodifiableList(wrappedAttributeNames);
		this.nonLazyAttributeNames = unmodifiableList(nonLazyAttributeNames);
		this.attributeTypes = unmodifiableMap(attributeTypes);
		this.componentPaths = unmodifiableMap(componentPaths);
		this.associations = unmodifiableMap(associations);
		this.additionalMetadatas = unmodifiableMap(additionalMetadatas);
	}

	@Override
	public List<String> getDeclaredAttributeNames() {
		return declaredAttributeNames;
	}

	@Override
	public List<String> getAttributeNames() {
		return unwrappedAttributeNames;
	}

	@Override
	public List<String> getWrappedAttributeNames() {
		return wrappedAttributeNames;
	}

	@Override
	public Class<?> getAttributeType(String attributeName) {
		assertAttributePresence(attributeName);
		return attributeTypes.get(attributeName);
	}

	@Override
	public List<String> getNonLazyAttributeNames() {
		return nonLazyAttributeNames;
	}

	@Override
	public boolean isAssociation(String attributeName) {
		return associations.containsKey(attributeName);
	}

	@Override
	public boolean isAssociationOptional(String associationName) {
		assertAttributePresence(associationName);
		return OptionalAssociation.class.isAssignableFrom(associations.get(associationName).getClass());
	}

	private void assertAttributePresence(String associationName) {
		if (!attributeTypes.containsKey(associationName)) {
			throw new IllegalArgumentException(String.format("Unknown attribute %s", associationName));
		}
	}

	@Override
	public Map<String, ComponentPath> getComponentPaths() {
		return componentPaths;
	}

	@Override
	public multicados.internal.domain.metadata.AssociationType getAssociationType(String attributeName) {
		assertAttributePresence(attributeName);
		return associations.get(attributeName).getType();
	}

	@Override
	public boolean isComponent(String attributeName) {
		assertAttributePresence(attributeName);
		return componentPaths.containsKey(attributeName);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <M extends DomainResourceMetadata<T>> M unwrap(Class<M> type) {
		if (type.equals(DomainResourceAttributesMetadata.class)) {
			return (M) this;
		}

		final M target = (M) additionalMetadatas.get(type);

		if (target == null) {
			throw new IllegalArgumentException(String.format("Metadata role %s not found", type));
		}

		return target;
	}

	public Map<String, Class<?>> getAttributeTypes() {
		return attributeTypes;
	}

	public Map<String, DomainAssociation> getAssociations() {
		return associations;
	}

	private <E> String collectList(List<E> list, Function<E, String> toString) {
		return list.size() == 0 ? "<<empty>>"
				: list.stream().map(toString)
						.collect(Collectors.joining(multicados.internal.helper.StringHelper.COMMON_JOINER));
	}

	private <K, V> String collectMap(Map<K, V> map, Function<Map.Entry<K, V>, String> toString, CharSequence joiner) {
		return map.size() == 0 ? "<<empty>>"
				: map.entrySet().stream().map(toString).collect(Collectors.joining(joiner));
	}

	@Override
	public String toString() {
		if (wrappedAttributeNames.isEmpty()) {
			return String.format("%s<%s>(<<empty>>)", this.getClass().getSimpleName(), getResourceType().getName());
		}
		// @formatter:off
		return String.format("%s<%s>(\n"
				+ "\tattributeNames=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ "\tenclosedAttributeNames=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ "\tnonLazyAttributes=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ "\tattributeTypes=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ "\tassociationAttributes=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ "\tcomponentPaths=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ ")",
				this.getClass().getSimpleName(), getResourceType().getName(),
				collectList(unwrappedAttributeNames, Function.identity()),
				collectList(wrappedAttributeNames, Function.identity()),
				collectList(nonLazyAttributeNames, Function.identity()),
				collectMap(attributeTypes, entry -> String.format("%s: %s", entry.getKey(), entry.getValue().getName()), "\n\t\t"),
				collectMap(associations, entry -> String.format("%s|%s", entry.getKey(), entry.getValue().getType()), "\n\t\t"),
				collectMap(componentPaths, entry -> String.format("%s: %s", entry.getKey(), entry.getValue()), "\n\t\t"));
		// @formatter:on
	}

	public interface DomainAssociation {

		String getName();

		multicados.internal.domain.metadata.AssociationType getType();

		public abstract class AbstractAssociation implements DomainAssociation {

			private final String name;
			private final multicados.internal.domain.metadata.AssociationType type;

			public AbstractAssociation(String name, multicados.internal.domain.metadata.AssociationType type) {
				this.name = Objects.requireNonNull(name);
				this.type = Objects.requireNonNull(type);
			}

			@Override
			public multicados.internal.domain.metadata.AssociationType getType() {
				return type;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public int hashCode() {
				return name.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}

				if (obj == null) {
					return false;
				}

				return name.equals(((DomainAssociation) obj).getName());
			}

		}

		public class OptionalAssociation extends AbstractAssociation {

			public OptionalAssociation(String name, multicados.internal.domain.metadata.AssociationType type) {
				super(name, type);
			}

		}

		public class MandatoryAssociation extends AbstractAssociation {

			public MandatoryAssociation(String name, multicados.internal.domain.metadata.AssociationType type) {
				super(name, type);
			}

		}

	}

}
