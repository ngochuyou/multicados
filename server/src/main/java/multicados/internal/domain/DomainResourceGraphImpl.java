/**
 * 
 */
package multicados.internal.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceGraphImpl<T extends DomainResource> implements DomainResourceGraph<T> {

	private final DomainResourceGraph<? super T> parent;
	private final Class<T> resourceType;
	private Set<DomainResourceGraph<? extends T>> childrens;

	private final int depth;

	public DomainResourceGraphImpl(Class<T> resourceType) {
		this(null, resourceType);
	}

	public DomainResourceGraphImpl(DomainResourceGraph<? super T> parent, Class<T> resourceType) {
		this.parent = parent;
		depth = parent == null ? 0 : parent.getDepth() + 1;
		this.resourceType = resourceType;
		this.childrens = new LinkedHashSet<>();
	}

	@Override
	public DomainResourceGraph<? super T> getParent() {
		return parent;
	}

	@Override
	public Class<T> getResourceType() {
		return resourceType;
	}

	@Override
	public Set<DomainResourceGraph<? extends T>> getChildrens() {
		return childrens;
	}

	@Override
	public void forEach(Utils.HandledConsumer<DomainResourceGraph<? extends DomainResource>, Exception> consumer)
			throws Exception {
		consumer.accept(this);

		for (final DomainResourceGraph<? extends T> children : childrens) {
			children.forEach(consumer);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public DomainResourceGraph<? extends T> add(Class<? extends DomainResource> childType) {
		if (resourceType.equals(childType.getSuperclass()) || TypeHelper.isImplementedFrom(childType, resourceType)) {
			final DomainResourceGraphImpl<? extends T> newChild = new DomainResourceGraphImpl<>(this,
					(Class<? extends T>) childType);

			childrens.add(newChild);

			return newChild;
		}

		DomainResourceGraph<? extends T> posibleChild = null;

		for (final DomainResourceGraph<? extends T> child : childrens) {
			if (posibleChild == null) {
				posibleChild = child.add(childType);
				continue;
			}

			child.add((DomainResourceGraph) posibleChild);
		}

		return posibleChild;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void add(DomainResourceGraph<? extends T> child) {
		Class<? extends T> childType = child.getResourceType();

		if (resourceType.equals(childType.getSuperclass()) || TypeHelper.isImplementedFrom(childType, resourceType)) {
			childrens.add(child);
			return;
		}

		childrens.forEach(childNode -> childNode.add((DomainResourceGraph) child));
	}

	@Override
	public DomainResourceGraph<? extends T> locate(Class<DomainResource> resourceType) {
		if (this.resourceType.equals(resourceType)) {
			return this;
		}

		if (childrens.isEmpty()) {
			return null;
		}

		DomainResourceGraph<? extends T> target;

		for (DomainResourceGraph<? extends T> child : childrens) {
			target = child.locate(resourceType);

			if (target != null) {
				return target;
			}
		}

		return null;
	}

	@SuppressWarnings("rawtypes")
	public <E, C extends Collection<E>> C collect(Supplier<C> factory, Function<DomainResourceGraph, E> mapper) {
		C collection = factory.get();

		collection.addAll(List.of(mapper.apply(this)));

		for (final DomainResourceGraph<? extends T> child : childrens) {
			collection.addAll(child.collect(factory, mapper));
		}

		return collection;
	}

	@Override
	public <E, C extends Collection<E>> C collect(DomainResourceGraphCollector<E, C> collector) {
		return collect(collector.getFactory(), collector.getMapper());
	}

	@Override
	public void doAfterContextBuild() {
		this.childrens = Collections.unmodifiableSet(this.childrens);
	}

	@Override
	public int hashCode() {
		return resourceType.hashCode();
	}

	@Override
	public int getDepth() {
		return depth;
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

		DomainResourceGraphImpl other = (DomainResourceGraphImpl) obj;

		return Objects.equals(resourceType, other.resourceType);
	}

}
