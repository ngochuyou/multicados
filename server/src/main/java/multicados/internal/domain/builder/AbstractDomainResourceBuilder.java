/**
 * 
 */
package multicados.internal.domain.builder;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

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

	private class CompositeDomainResourceBuilder<T extends D> extends AbstractDomainResourceBuilder<T> {

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
		public T buildInsertion(Serializable id, T resource, EntityManager entityManager) throws Exception {
			return childBuilder.buildInsertion(id, (T) parentBuilder.buildInsertion(id, resource, entityManager),
					entityManager);
		}

		@SuppressWarnings("unchecked")
		@Override
		public T buildUpdate(Serializable id, T model, T resource, EntityManager entityManger) {
			return childBuilder.buildUpdate(id, model, (T) parentBuilder.buildUpdate(id, model, resource, entityManger),
					entityManger);
		}

		@Override
		public String getLoggableName() {
			return String.format("[%s, %s]", parentBuilder.getLoggableName(), childBuilder.getLoggableName());
		}

		@Override
		public void doAfterContextBuild() {
			parentBuilder.doAfterContextBuild();
			childBuilder.doAfterContextBuild();
		}

	}

	@Override
	public synchronized void doAfterContextBuild() {
		if (access == null) {
			return;
		}

		LoggerFactory.getLogger(this.getClass()).trace(Access.getClosingMessage(this));
		access = null;
	}

}
