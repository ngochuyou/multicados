/**
 *
 */
package multicados.internal.domain;

import java.util.Collection;
import java.util.Deque;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import multicados.internal.context.ContextBuildListener;
import multicados.internal.helper.Utils.HandledConsumer;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceGraph<T extends DomainResource> extends ContextBuildListener {

	Set<DomainResourceGraph<? super T>> getParents();

	Class<T> getResourceType();

	Set<DomainResourceGraph<? extends T>> getChildrens();

	<E extends T> void add(DomainResourceGraph<E> graph);

	void forEach(HandledConsumer<DomainResourceGraph<?>, Exception> consumer) throws Exception;

	<E extends T> DomainResourceGraph<E> locate(Class<E> resourceType);

	<E, C extends Collection<E>> C collect(Supplier<C> factory, Function<DomainResourceGraph<?>, E> mapper);

	<E, C extends Collection<E>> C collect(DomainResourceGraphCollector<E, C> collector);

	Deque<Class<? extends DomainResource>> getClassInheritance();

	Deque<DomainResourceGraph<? extends DomainResource>> getGraphInheritance();

	interface DomainResourceGraphCollector<E, C extends Collection<E>> {

		Supplier<C> getFactory();

		Function<DomainResourceGraph<?>, E> getMapper();

	}

	abstract static class AbstractCollector<E, C extends Collection<E>> implements DomainResourceGraphCollector<E, C> {
		private final Supplier<C> factory;
		private final Function<DomainResourceGraph<?>, E> mapper;

		public AbstractCollector(Supplier<C> supplier, Function<DomainResourceGraph<?>, E> mapper) {
			super();
			this.factory = supplier;
			this.mapper = mapper;
		}

		@Override
		public Supplier<C> getFactory() {
			return factory;
		}

		@Override
		public Function<DomainResourceGraph<?>, E> getMapper() {
			return mapper;
		}

	}

}
