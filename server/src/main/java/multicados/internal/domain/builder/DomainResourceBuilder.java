/**
 *
 */
package multicados.internal.domain.builder;

import java.io.Serializable;

import javax.persistence.EntityManager;

import multicados.internal.context.ContextBuildListener;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.GraphLogic;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceBuilder<T extends DomainResource> extends GraphLogic<T>, ContextBuildListener {

	/**
	 * Occurs only when the entity is being inserted
	 * </p>
	 * <em>Example:</em> While inserting an user we always hash the password.
	 * Whereas an update has to perform a check to determine if the user is updating
	 * their password or not, then make the decision to hash/update that password
	 *
	 * @param model
	 * @return entity {@link DomainResource}
	 * @throws Exception
	 */
	T buildInsertion(Serializable id, T model, EntityManager entityManager) throws Exception;

	/**
	 * @param id
	 * @param model the requested data to update
	 * @param resource the persistent entity
	 * @param entityManger
	 * @return persisted {@link DomainResource}
	 */
	T buildUpdate(Serializable id, T model, T persistence, EntityManager entityManger);

	<E extends T> DomainResourceBuilder<E> and(DomainResourceBuilder<E> next);

	@Override
	default <E extends T> GraphLogic<E> and(GraphLogic<E> next) {
		return and((DomainResourceBuilder<E>) next);
	}

}
