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
import multicados.internal.domain.DomainResource;
import multicados.internal.service.DomainService;
import multicados.internal.service.ServiceResult;

/**
 * @author Ngoc Huy
 *
 */
public interface GenericCRUDService<TUPLE, EM extends EntityManager> extends DomainService, ContextBuilder {

	default <E extends DomainResource> ServiceResult create(Serializable id, E model, Class<E> type, EM entityManager) {
		return create(id, model, type, entityManager, false);
	}

	<E extends DomainResource> ServiceResult create(Serializable id, E model, Class<E> type, EM entityManager,
			boolean flushOnFinish);

	default <E extends DomainResource> ServiceResult update(Serializable id, E model, Class<E> type, EM entityManager) {
		return update(id, model, type, entityManager, false);
	}

	<E extends DomainResource> ServiceResult update(Serializable id, E model, Class<E> type, EM entityManager,
			boolean flushOnFinish);

	/* ==================== */
	<E extends DomainResource> List<TUPLE> readAll(Class<E> type, Collection<String> properties, Pageable pageable,
			GrantedAuthority credential, EM entityManager) throws Exception;

	<E extends DomainResource> List<TUPLE> readAll(Class<E> type, Collection<String> properties,
			GrantedAuthority credential, EM entityManager) throws Exception;

	<E extends DomainResource> List<TUPLE> readAll(Class<E> type, Collection<String> properties,
			Specification<E> specification, Pageable pageable, GrantedAuthority credential, EM entityManager)
			throws Exception;

	<E extends DomainResource> List<TUPLE> readAll(Class<E> type, Collection<String> properties,
			Specification<E> specification, GrantedAuthority credential, EM entityManager) throws Exception;

	/* ==================== */
	<E extends DomainResource> TUPLE readOne(Class<E> type, Collection<String> properties,
			Specification<E> specification, GrantedAuthority credential, EM entityManager) throws Exception;

	/* ==================== */
	<E extends DomainResource> TUPLE readById(Class<E> type, Serializable id, Collection<String> properties,
			GrantedAuthority credential, EM entityManager) throws Exception;
}
