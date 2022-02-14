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
public interface DomainResourceTree<T extends DomainResource> extends ContextBuildListener {

	DomainResourceTree<? super T> getParent();

	Class<T> getResourceType();

	Set<DomainResourceTree<? extends T>> getChildrens();

	void add(Class<? extends DomainResource> resourceType);

	void forEach(HandledConsumer<DomainResourceTree<? extends DomainResource>, Exception> consumer) throws Exception;

	DomainResourceTree<? extends T> locate(Class<DomainResource> resourceType);

	@SuppressWarnings("rawtypes")
	<E, C extends Collection<E>> C collect(Supplier<C> factory, Function<DomainResourceTree, E> mapper);

	<E, C extends Collection<E>> C collect(DomainResourceTreeCollector<E, C> collector);
	
	interface DomainResourceTreeCollector<E, C extends Collection<E>> {

		Supplier<C> getFactory();

		@SuppressWarnings("rawtypes")
		Function<DomainResourceTree, E> getMapper();

	}

	@SuppressWarnings("rawtypes")
	abstract static class AbstractCollector<E, C extends Collection<E>> implements DomainResourceTreeCollector<E, C> {
		private final Supplier<C> factory;
		private final Function<DomainResourceTree, E> mapper;

		public AbstractCollector(Supplier<C> supplier, Function<DomainResourceTree, E> mapper) {
			super();
			this.factory = supplier;
			this.mapper = mapper;
		}

		public Supplier<C> getFactory() {
			return factory;
		}

		public Function<DomainResourceTree, E> getMapper() {
			return mapper;
		}

	}

}
