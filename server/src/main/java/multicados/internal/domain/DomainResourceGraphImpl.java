/**
 * 
 */
package multicados.internal.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.util.Assert;

import multicados.internal.helper.FunctionHelper.HandledConsumer;
import multicados.internal.helper.TypeHelper;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceGraphImpl<T extends DomainResource> implements DomainResourceGraph<T> {

	private final DomainResourceGraph<? super T> parent;
	private final Class<T> resourceType;
	private Set<DomainResourceGraph<? extends T>> childrens;

	public DomainResourceGraphImpl(Class<T> resourceType) {
		this(null, resourceType);
	}

	public DomainResourceGraphImpl(DomainResourceGraph<? super T> parent, Class<T> resourceType) {
		this(parent, resourceType, new HashSet<>(0));
	}

	private DomainResourceGraphImpl(DomainResourceGraph<? super T> parent, DomainResourceGraph<T> child) {
		this.parent = parent;
		resourceType = child.getResourceType();
		childrens = child.getChildrens();
	}

	public DomainResourceGraphImpl(DomainResourceGraph<? super T> parent, Class<T> resourceType,
			Set<DomainResourceGraph<? extends T>> childrens) {
		this.parent = parent;
		this.resourceType = resourceType;

		if (childrens == null) {
			this.childrens = new HashSet<>();
			return;
		}

		Set<DomainResourceGraph<? extends T>> associationSafeChilds = new HashSet<>();

		for (DomainResourceGraph<? extends T> child : childrens) {
			associationSafeChilds.add(new DomainResourceGraphImpl<>(this, child));
		}

		this.childrens = associationSafeChilds;
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
	public void forEach(HandledConsumer<DomainResourceGraph<? extends DomainResource>, Exception> consumer)
			throws Exception {
		consumer.accept(this);

		for (DomainResourceGraph<? extends T> children : childrens) {
			children.forEach(consumer);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void add(Class<? extends DomainResource> resourceType) {
		Assert.notNull(resourceType, "Resource must not be null");

		if (this.resourceType.equals(resourceType.getSuperclass())
				|| TypeHelper.isImplementedFrom(resourceType, this.resourceType)) {
			childrens.add(new DomainResourceGraphImpl<>(this, (Class<? extends T>) resourceType));
			return;
		}

		childrens.forEach(node -> node.add(resourceType));
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

		for (DomainResourceGraph<? extends T> child : childrens) {
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

		DomainResourceGraphImpl other = (DomainResourceGraphImpl) obj;

		return Objects.equals(resourceType, other.resourceType);
	}

}
