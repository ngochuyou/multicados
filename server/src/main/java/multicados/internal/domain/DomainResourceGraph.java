/**
 * 
 */
package multicados.internal.domain;

import java.util.Collection;
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

	@Deprecated
	DomainResourceGraph<? super T> getParent();

	Set<DomainResourceGraph<? super T>> getParents();

	Class<T> getResourceType();

	Set<DomainResourceGraph<? extends T>> getChildrens();

	<E extends T> void add(DomainResourceGraph<E> graph);

	void forEach(HandledConsumer<DomainResourceGraph<?>, Exception> consumer) throws Exception;

	<E extends T> DomainResourceGraph<E> locate(Class<E> resourceType);

	@SuppressWarnings("rawtypes")
	<E, C extends Collection<E>> C collect(Supplier<C> factory, Function<DomainResourceGraph, E> mapper);

	<E, C extends Collection<E>> C collect(DomainResourceGraphCollector<E, C> collector);

	interface DomainResourceGraphCollector<E, C extends Collection<E>> {

		Supplier<C> getFactory();

		@SuppressWarnings("rawtypes")
		Function<DomainResourceGraph, E> getMapper();

	}

	@SuppressWarnings("rawtypes")
	abstract static class AbstractCollector<E, C extends Collection<E>> implements DomainResourceGraphCollector<E, C> {
		private final Supplier<C> factory;
		private final Function<DomainResourceGraph, E> mapper;

		public AbstractCollector(Supplier<C> supplier, Function<DomainResourceGraph, E> mapper) {
			super();
			this.factory = supplier;
			this.mapper = mapper;
		}

		public Supplier<C> getFactory() {
			return factory;
		}

		public Function<DomainResourceGraph, E> getMapper() {
			return mapper;
		}

	}

}
