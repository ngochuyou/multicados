/**
 *
 */
package multicados.internal.domain;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceGraphImpl<T extends DomainResource> implements DomainResourceGraph<T> {

	private static final Logger logger = LoggerFactory.getLogger(DomainResourceGraphImpl.class);

	private final Class<T> resourceType;

	private Set<DomainResourceGraph<? super T>> parents;
	private Set<DomainResourceGraph<? extends T>> childrens;

	public DomainResourceGraphImpl(Class<T> resourceType) {
		this.resourceType = resourceType;
		parents = new LinkedHashSet<>(0);
		childrens = new LinkedHashSet<>(0);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <E extends T> void add(DomainResourceGraph<E> child) {
		final Class<E> childType = child.getResourceType();

		if (resourceType.equals(childType)) {
			if (logger.isWarnEnabled()) {
				logger.warn("Skipping an exsiting graph");
			}

			return;
		}

		if (resourceType.equals(childType.getSuperclass()) || TypeHelper.isImplementedFrom(childType, resourceType)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Located parent type {} of entry type {} in graph", resourceType.getName(),
						childType.getName());
			}

			child.getParents().add(this);
			childrens.add(child);
			return;
		}

		if (childrens.isEmpty()) {
			return;
		}

		for (final DomainResourceGraph<?> children : childrens) {
			children.add((DomainResourceGraph) child);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <E extends T> DomainResourceGraph<E> locate(Class<E> resourceType) {
		if (this.resourceType.equals(resourceType)) {
			return (DomainResourceGraph<E>) this;
		}

		if (childrens.isEmpty()) {
			return null;
		}

		DomainResourceGraph target;

		for (DomainResourceGraph child : childrens) {
			target = child.locate(resourceType);

			if (target != null) {
				return target;
			}
		}

		return null;
	}

	private <E> Deque<E> getInheritance(Function<DomainResourceGraph<?>, E> mapper) {
		final Deque<Set<DomainResourceGraph<?>>> cache = getInheritance(new ArrayDeque<>());
		final DistinctInheritanceDeque<E> queue = new DistinctInheritanceDeque<>();

		while (!cache.isEmpty()) {
			for (DomainResourceGraph<?> graph : cache.pop()) {
				queue.add(mapper.apply(graph));
			}
		}

		return queue;
	}

	private Deque<Set<DomainResourceGraph<?>>> getInheritance(Deque<Set<DomainResourceGraph<?>>> cache) {
		cache.push(Set.of(this));

		if (parents == null) {
			return cache;
		}

		for (final DomainResourceGraph<? super T> parent : parents) {
			((DomainResourceGraphImpl<? super T>) parent).getInheritance(cache);
		}

		return cache;
	}

	@Override
	public Deque<Class<? extends DomainResource>> getClassInheritance() {
		return getInheritance(DomainResourceGraph::getResourceType);
	}

	@Override
	public Deque<DomainResourceGraph<? extends DomainResource>> getGraphInheritance() {
		return getInheritance(Function.identity());
	}

	@Override
	public Set<DomainResourceGraph<? super T>> getParents() {
		return parents;
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

		for (final DomainResourceGraph<?> children : childrens) {
			children.forEach(consumer);
		}
	}

	@Override
	public <E, C extends Collection<E>> C collect(Supplier<C> factory, Function<DomainResourceGraph<?>, E> mapper) {
		return collect(factory, mapper, false);
	}

	public <E, C extends Collection<E>> C collect(Supplier<C> factory, Function<DomainResourceGraph<?>, E> mapper,
			boolean bottomUp) {
		final C collection = factory.get();

		if (parents != null) {
			for (final DomainResourceGraph<? super T> parentGraph : parents) {
				collection.addAll((((DomainResourceGraphImpl<? super T>) parentGraph)).collect(factory, mapper, true));
			}
		}

		collection.add(mapper.apply(this));

		if (!bottomUp) {
			for (final DomainResourceGraph<?> child : childrens) {
				collection.addAll(child.collect(factory, mapper));
			}
		}

		return collection;
	}

	@Override
	public <E, C extends Collection<E>> C collect(DomainResourceGraphCollector<E, C> collector) {
		return collect(collector.getFactory(), collector.getMapper());
	}

	@Override
	public void doAfterContextBuild() {
		parents = CollectionHelper.isEmpty(parents) ? null : Collections.unmodifiableSet(parents);
		childrens = Collections.unmodifiableSet(childrens);
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

		if ((obj == null) || (getClass() != obj.getClass()))
			return false;

		DomainResourceGraphImpl other = (DomainResourceGraphImpl) obj;

		return Objects.equals(resourceType, other.resourceType);
	}

	private static class DistinctInheritanceDeque<E> extends ArrayDeque<E> {

		private static final long serialVersionUID = 1L;

		private final Set<E> set = new HashSet<>();

		@Override
		public boolean contains(Object o) {
			return set.contains(o);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return set.containsAll(c);
		}

		@Override
		public boolean add(E e) {
			if (contains(e)) {
				return false;
			}

			set.add(e);
			return super.add(e);
		}

		@Override
		public void push(E e) {
			if (contains(e)) {
				return;
			}

			set.add(e);
			super.push(e);
		}

	}

}
