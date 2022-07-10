/**
 *
 */
package multicados.internal.domain.builder;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import multicados.internal.domain.AbstractGraphLogicsFactory;
import multicados.internal.domain.DomainResource;
import multicados.internal.helper.Utils.Access;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractDomainResourceBuilder<D extends DomainResource> implements DomainResourceBuilder<D> {

	private volatile Access access = new Access() {};

	@Override
	public <E extends D> DomainResourceBuilder<E> and(DomainResourceBuilder<E> next) {
		Assert.notNull(access, Access.getClosingMessage(this));
		return new CompositeDomainResourceBuilder<>(this, next);
	}

	private static class CompositeDomainResourceBuilder<D extends DomainResource, T extends D>
			extends AbstractDomainResourceBuilder<T> implements AbstractGraphLogicsFactory.FixedLogic {

		private final DomainResourceBuilder<D> parentBuilder;
		private final DomainResourceBuilder<T> childBuilder;

		public CompositeDomainResourceBuilder(DomainResourceBuilder<D> parentBuilder,
				DomainResourceBuilder<T> childBuilder) {
			super();
			this.parentBuilder = parentBuilder;
			this.childBuilder = childBuilder;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T buildInsertion(T model, EntityManager entityManager) throws Exception {
			return childBuilder.buildInsertion((T) parentBuilder.buildInsertion(model, entityManager), entityManager);
		}

		@SuppressWarnings("unchecked")
		@Override
		public T buildUpdate(T model, T persistence, EntityManager entityManger) {
			return childBuilder.buildUpdate(model, (T) parentBuilder.buildUpdate(model, persistence, entityManger),
					entityManger);
		}

		@Override
		public String getLoggableName() {
			return String.format("[%s, %s]", parentBuilder.getLoggableName(), childBuilder.getLoggableName());
		}

		@Override
		public void doAfterContextBuild() throws IllegalAccessException {
			parentBuilder.doAfterContextBuild();
			childBuilder.doAfterContextBuild();
		}

	}

	@Override
	public synchronized void doAfterContextBuild() throws IllegalAccessException {
		if (access == null) {
			throw new IllegalAccessException(Access.getClosedMessage(this));
		}

		LoggerFactory.getLogger(this.getClass()).trace(Access.getClosingMessage(this));
		access = null;
	}

}
