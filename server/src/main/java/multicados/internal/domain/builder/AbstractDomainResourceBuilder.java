/**
 * 
 */
package multicados.internal.domain.builder;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import multicados.internal.domain.DomainResource;
import multicados.internal.helper.Utils.Access;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractDomainResourceBuilder<D extends DomainResource> implements DomainResourceBuilder<D> {

	private final Access access = new Access() {};

	@Override
	public <E extends D> DomainResourceBuilder<E> and(DomainResourceBuilder<E> next) {
		Assert.notNull(access, Access.CLOSED_MESSAGE);

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

		@Override
		public <E extends T> E buildInsertion(Serializable id, E resource, EntityManager entityManager)
				throws Exception {
			return childBuilder.buildInsertion(id, parentBuilder.buildInsertion(id, resource, entityManager),
					entityManager);
		}

		@Override
		public <E extends T> E buildUpdate(Serializable id, E model, E resource, EntityManager entityManger) {
			return childBuilder.buildUpdate(id, model, parentBuilder.buildUpdate(id, model, resource, entityManger),
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
	public void doAfterContextBuild() {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		logger.trace(Access.getClosingMessage(this));
	}

}
