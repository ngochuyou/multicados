/**
 * 
 */
package multicados.internal.domain;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import multicados.internal.context.ContextBuildListener;
import multicados.internal.helper.FunctionHelper.HandledConsumer;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceGraph<T extends DomainResource> extends ContextBuildListener {

	DomainResourceGraph<? super T> getParent();

	Class<T> getResourceType();

	Set<DomainResourceGraph<? extends T>> getChildrens();

	void add(Class<? extends DomainResource> resourceType);

	void forEach(HandledConsumer<DomainResourceGraph<? extends DomainResource>, Exception> consumer) throws Exception;

	DomainResourceGraph<? extends T> locate(Class<DomainResource> resourceType);

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
