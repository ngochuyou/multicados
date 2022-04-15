/**
 * 
 */
package multicados.internal.domain.repository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;

import org.hibernate.SharedSessionContract;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface GenericRepository extends ContextBuilder {

	<D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector,
			SharedSessionContract session) throws Exception;

	<D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector,
			LockModeType lockModeType, SharedSessionContract session) throws Exception;

	<D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector, Pageable pageable,
			SharedSessionContract session) throws Exception;

	<D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector, Pageable pageable,
			LockModeType lockMode, SharedSessionContract session) throws Exception;

	<D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector, Specification<D> spec,
			SharedSessionContract session) throws Exception;

	<D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector, Specification<D> spec,
			LockModeType lockModeType, SharedSessionContract session) throws Exception;

	<D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector, Specification<D> spec,
			Pageable pageable, SharedSessionContract session) throws Exception;

	<D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector, Specification<D> spec,
			Pageable pageable, LockModeType lockMode, SharedSessionContract session) throws Exception;

	/* ==================== */
	<D extends DomainResource> List<D> findAll(Class<D> type, SharedSessionContract session) throws Exception;

	<D extends DomainResource> List<D> findAll(Class<D> type, LockModeType lockModeType, SharedSessionContract session)
			throws Exception;

	<D extends DomainResource> List<D> findAll(Class<D> type, Pageable pageable, SharedSessionContract session)
			throws Exception;

	<D extends DomainResource> List<D> findAll(Class<D> type, Pageable pageable, LockModeType lockMode,
			SharedSessionContract session) throws Exception;

	<D extends DomainResource> List<D> findAll(Class<D> type, SharedSessionContract session, Specification<D> spec)
			throws Exception;

	<D extends DomainResource> List<D> findAll(Class<D> type, LockModeType lockModeType, SharedSessionContract session,
			Specification<D> spec) throws Exception;

	<D extends DomainResource> List<D> findAll(Class<D> type, Pageable pageable, SharedSessionContract session,
			Specification<D> spec) throws Exception;

	<D extends DomainResource> List<D> findAll(Class<D> type, Pageable pageable, LockModeType lockMode,
			SharedSessionContract session, Specification<D> spec) throws Exception;

	/* ==================== */
	<D extends DomainResource> Optional<D> findById(Class<D> clazz, Serializable id, SharedSessionContract session)
			throws Exception;

	<D extends DomainResource> Optional<D> findById(Class<D> clazz, Serializable id, LockModeType lockMode,
			SharedSessionContract session) throws Exception;

	/* ==================== */
	<D extends DomainResource> Optional<Tuple> findById(Class<D> clazz, Serializable id, Selector<D, Tuple> selector,
			SharedSessionContract session) throws Exception;

	<D extends DomainResource> Optional<Tuple> findById(Class<D> clazz, Serializable id, Selector<D, Tuple> selector,
			LockModeType lockMode, SharedSessionContract session) throws Exception;

	/* ==================== */
	<D extends DomainResource> Optional<D> findOne(Class<D> type, Specification<D> spec, SharedSessionContract session)
			throws Exception;

	<D extends DomainResource> Optional<D> findOne(Class<D> type, Specification<D> spec, LockModeType lockMode,
			SharedSessionContract session) throws Exception;

	/* ==================== */
	<D extends DomainResource> Optional<Tuple> findOne(Class<D> type, Selector<D, Tuple> selector,
			Specification<D> spec, SharedSessionContract session) throws Exception;

	<D extends DomainResource> Optional<Tuple> findOne(Class<D> type, Selector<D, Tuple> selector,
			Specification<D> spec, LockModeType lockMode, SharedSessionContract session) throws Exception;

}
