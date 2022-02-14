/**
 * 
 */
package multicados.internal.domain.builder;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.hibernate.SharedSessionContract;

import multicados.internal.context.ContextBuildListener;
import multicados.internal.context.Loggable;
import multicados.internal.domain.DomainResource;
import multicados.internal.helper.HibernateHelper;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceBuilder<T extends DomainResource> extends ContextBuildListener, Loggable {

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
	default <E extends T> E buildInsertion(Serializable id, E resource) throws Exception {
		return buildInsertion(id, resource, HibernateHelper.getCurrentSession());
	}

	<E extends T> E buildInsertion(Serializable id, E resource, EntityManager entityManager) throws Exception;

	/**
	 * @see DomainResourceBuilder#insertionBuild(DomainResource)
	 * 
	 * @param model
	 * @return persisted {@link DomainResource}
	 */
	default <E extends T> E buildUpdate(Serializable id, E model, E resource) {
		return buildUpdate(id, resource, resource, HibernateHelper.getCurrentSession());
	}

	<E extends T> E buildUpdate(Serializable id, E model, E resource, SharedSessionContract session);

	<E extends T> DomainResourceBuilder<E> and(DomainResourceBuilder<E> next);

	<E extends T> boolean contains(DomainResourceBuilder<E> candidate);
	
}
