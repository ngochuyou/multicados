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
import multicados.internal.domain.Entity;

/**
 * @author Ngoc Huy
 *
 */
public interface GenericRepository extends ContextBuilder {

	<S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			LockModeType lockModeType, SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			Pageable pageable, SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			Pageable pageable, LockModeType lockMode, SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			Specification<T> spec, SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			Specification<T> spec, LockModeType lockModeType, SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			Specification<T> spec, Pageable pageable, SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			Specification<T> spec, Pageable pageable, LockModeType lockMode, SharedSessionContract session)
			throws Exception;

	/* ==================== */
	<S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, SharedSessionContract session)
			throws Exception;

	<S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, LockModeType lockModeType,
			SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, Pageable pageable,
			SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, Pageable pageable,
			LockModeType lockMode, SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, SharedSessionContract session,
			Specification<T> spec) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, LockModeType lockModeType,
			SharedSessionContract session, Specification<T> spec) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, Pageable pageable,
			SharedSessionContract session, Specification<T> spec) throws Exception;

	<S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, Pageable pageable,
			LockModeType lockMode, SharedSessionContract session, Specification<T> spec) throws Exception;

	/* ==================== */
	<S extends Serializable, T extends Entity<S>> Optional<T> findById(Class<T> clazz, Serializable id,
			SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> Optional<T> findById(Class<T> clazz, Serializable id,
			LockModeType lockMode, SharedSessionContract session) throws Exception;

	/* ==================== */
	<S extends Serializable, T extends Entity<S>> Optional<Tuple> findById(Class<T> clazz, Serializable id,
			Selector<T, Tuple> selector, SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> Optional<Tuple> findById(Class<T> clazz, Serializable id,
			Selector<T, Tuple> selector, LockModeType lockMode, SharedSessionContract session) throws Exception;

	/* ==================== */
	<S extends Serializable, T extends Entity<S>> Optional<T> findOne(Class<T> type, Specification<T> spec,
			SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> Optional<T> findOne(Class<T> type, Specification<T> spec,
			LockModeType lockMode, SharedSessionContract session) throws Exception;

	/* ==================== */
	<S extends Serializable, T extends Entity<S>> Optional<Tuple> findOne(Class<T> type, Selector<T, Tuple> selector,
			Specification<T> spec, SharedSessionContract session) throws Exception;

	<S extends Serializable, T extends Entity<S>> Optional<Tuple> findOne(Class<T> type, Selector<T, Tuple> selector,
			Specification<T> spec, LockModeType lockMode, SharedSessionContract session) throws Exception;

}
