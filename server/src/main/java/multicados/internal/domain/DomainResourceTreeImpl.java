/**
 * 
 */
package multicados.internal.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.util.Assert;

import multicados.internal.helper.FunctionHelper.HandledConsumer;
import multicados.internal.helper.TypeHelper;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceTreeImpl<T extends DomainResource> implements DomainResourceTree<T> {

	private final DomainResourceTree<? super T> parent;
	private final Class<T> resourceType;
	private Set<DomainResourceTree<? extends T>> childrens;

	public DomainResourceTreeImpl(Class<T> resourceType) {
		this(null, resourceType);
	}

	public DomainResourceTreeImpl(DomainResourceTree<? super T> parent, Class<T> resourceType) {
		this(parent, resourceType, new HashSet<>(0));
	}

	public DomainResourceTreeImpl(DomainResourceTree<? super T> parent, Class<T> resourceType,
			Set<DomainResourceTree<? extends T>> childrens) {
		this.parent = parent;
		this.resourceType = resourceType;
		this.childrens = Optional.ofNullable(childrens).orElse(new HashSet<>(0));
	}

	@Override
	public DomainResourceTree<? super T> getParent() {
		return parent;
	}

	@Override
	public Class<T> getResourceType() {
		return resourceType;
	}

	@Override
	public Set<DomainResourceTree<? extends T>> getChildrens() {
		return childrens;
	}

	@Override
	public void forEach(HandledConsumer<DomainResourceTree<? extends DomainResource>, Exception> consumer)
			throws Exception {
		consumer.accept(this);

		for (DomainResourceTree<? extends T> children : childrens) {
			children.forEach(consumer);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void add(Class<? extends DomainResource> resourceType) {
		Assert.notNull(resourceType, "Resource must not be null");

		if (this.resourceType.equals(resourceType.getSuperclass())
				|| TypeHelper.isImplementedFrom(resourceType, DomainResource.class)) {
			childrens.add(new DomainResourceTreeImpl<>(this, (Class<? extends T>) resourceType));
			return;
		}

		childrens.forEach(node -> node.add(resourceType));
	}

	@Override
	public DomainResourceTree<? extends T> locate(Class<DomainResource> resourceType) {
		if (this.resourceType.equals(resourceType)) {
			return this;
		}

		if (childrens.isEmpty()) {
			return null;
		}

		DomainResourceTree<? extends T> target;

		for (DomainResourceTree<? extends T> child : childrens) {
			target = child.locate(resourceType);

			if (target != null) {
				return target;
			}
		}

		return null;
	}

	@Override
	public void doAfterContextBuild() {
		this.childrens = Collections.unmodifiableSet(this.childrens);
	}

	@Override
	public String toString() {
		return String.format("%s(parent=%s, type=%s, childrens=%d)", this.getClass().getSimpleName(),
				Optional.ofNullable(parent).map(p -> p.getResourceType().getName()).orElse("none"),
				resourceType.getName(), childrens.size());
	}

	@Override
	public int hashCode() {
		return resourceType.hashCode();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		DomainResourceTreeImpl other = (DomainResourceTreeImpl) obj;

		return Objects.equals(resourceType, other.resourceType);
	}

}
