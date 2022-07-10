/**
 *
 */
package multicados.internal.service.crud;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.IdentifiableResource;
import multicados.internal.service.DomainService;
import multicados.internal.service.ServiceResult;

/**
 * @author Ngoc Huy
 *
 */
public interface GenericCRUDService<TUPLE, EM extends EntityManager> extends DomainService, ContextBuilder {

	default <S extends Serializable, E extends IdentifiableResource<S>> ServiceResult create(Serializable id, E model,
			Class<E> type, EM entityManager) {
		return create(type, id, model, entityManager, false);
	}

	<S extends Serializable, E extends IdentifiableResource<S>> ServiceResult create(Class<E> type, Serializable id,
			E model, EM entityManager, boolean flushOnFinish);

	default <S extends Serializable, E extends IdentifiableResource<S>> ServiceResult update(Serializable id, E model,
			Class<E> type, EM entityManager) {
		return update(type, id, model, entityManager, false);
	}

	<S extends Serializable, E extends IdentifiableResource<S>> ServiceResult update(Class<E> type, Serializable id,
			E model, EM entityManager, boolean flushOnFinish);

	/* ==================== */
	<S extends Serializable, E extends IdentifiableResource<S>> List<TUPLE> readAll(Class<E> type,
			Collection<String> properties, Pageable pageable, GrantedAuthority credential, EM entityManager)
			throws Exception;

	<S extends Serializable, E extends IdentifiableResource<S>> List<TUPLE> readAll(Class<E> type,
			Collection<String> properties, GrantedAuthority credential, EM entityManager) throws Exception;

	<S extends Serializable, E extends IdentifiableResource<S>> List<TUPLE> readAll(Class<E> type,
			Collection<String> properties, Specification<E> specification, Pageable pageable,
			GrantedAuthority credential, EM entityManager) throws Exception;

	<S extends Serializable, E extends IdentifiableResource<S>> List<TUPLE> readAll(Class<E> type,
			Collection<String> properties, Specification<E> specification, GrantedAuthority credential,
			EM entityManager) throws Exception;

	/* ==================== */
	<S extends Serializable, E extends IdentifiableResource<S>> TUPLE readOne(Class<E> type,
			Collection<String> properties, Specification<E> specification, GrantedAuthority credential,
			EM entityManager) throws Exception;

	/* ==================== */
	<S extends Serializable, E extends IdentifiableResource<S>> TUPLE readById(Class<E> type, Serializable id,
			Collection<String> properties, GrantedAuthority credential, EM entityManager) throws Exception;
}
