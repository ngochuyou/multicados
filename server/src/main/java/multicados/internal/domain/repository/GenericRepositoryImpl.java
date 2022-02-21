/**
 * 
 */
package multicados.internal.domain.repository;

import static multicados.internal.helper.Utils.declare;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import org.hibernate.SharedSessionContract;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.ClassUtils;

import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraph;
import multicados.internal.domain.DomainResourceGraphCollectors;
import multicados.internal.domain.Entity;
import multicados.internal.domain.PermanentResource;
import multicados.internal.helper.SpecificationHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class GenericRepositoryImpl implements GenericRepository {

	private static final Pageable DEFAULT_PAGEABLE = Pageable.ofSize(50);
	private static final LockModeType DEFAULT_LOCK_MODE = LockModeType.NONE;

	private final Map<Class<? extends Entity<?>>, Specification<? extends Entity<?>>> fixedSpecifications;

	private final CriteriaBuilder criteriaBuilder;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public GenericRepositoryImpl(DomainResourceContext resourceContextProvider, SessionFactoryImplementor sfi)
			throws Exception {
		final Logger logger = LoggerFactory.getLogger(GenericRepositoryImpl.class);

		Map<Class<? extends Entity<?>>, Specification<? extends Entity<?>>> fixedSpecifications = new HashMap<>(0);

		for (DomainResourceGraph<Entity> node : resourceContextProvider.getEntityGraph()
				.collect(DomainResourceGraphCollectors.toGraphsSet())) {
			Class<? extends Entity<?>> entityType = (Class<? extends Entity<?>>) node.getResourceType();

			if (Modifier.isAbstract(entityType.getModifiers())) {
				logger.trace("Skipping abstract {} type {}", Entity.class.getSimpleName(), entityType.getName());
				break;
			}
			// @formatter:off
			Utils.declare(getAllInterfaces(entityType))
				.then(this::filterOutNonTargetedTypes)
				.then(ArrayList::new)
				.then(this::chainFixedSpecifications)
					.second(entityType)
				.biInverse()
				.identical(fixedSpecifications::put);
			// @formatter:on
		}

		this.fixedSpecifications = Collections.unmodifiableMap(fixedSpecifications);
		criteriaBuilder = sfi.getCriteriaBuilder();
	}

	private <T extends Entity<?>> Set<Class<?>> getAllInterfaces(Class<T> entityType) {
		final Logger logger = LoggerFactory.getLogger(GenericRepositoryImpl.class);

		logger.trace("Finding all interfaces of {} type [{}]", Entity.class.getSimpleName(),
				entityType.getSimpleName());

		return ClassUtils.getAllInterfacesForClassAsSet(entityType);
	}

	private Set<Class<?>> filterOutNonTargetedTypes(Set<Class<?>> interfaces) {
		final Logger logger = LoggerFactory.getLogger(GenericRepositoryImpl.class);

		logger.trace("Filtering fixed interfaces");

		return interfaces.stream().filter(FIXED_SPECIFICATIONS::containsKey).collect(Collectors.toSet());
	}

	@SuppressWarnings("rawtypes")
	private static final Map<Class, Specification> FIXED_SPECIFICATIONS = Map.of(PermanentResource.class,
			(root, query, builder) -> builder.equal(root.get("active"), Boolean.TRUE));

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Specification chainFixedSpecifications(List<Class<?>> interfaces) {
		final Logger logger = LoggerFactory.getLogger(GenericRepositoryImpl.class);

		logger.trace("Chaining {}(s)", Specification.class.getSimpleName());
		// shouldn't be null here
		if (interfaces.isEmpty()) {
			return SpecificationHelper.none();
		}
		// @formatter:off
		return IntStream.range(1, interfaces.size())
					.mapToObj(interfaces::get)
					.map(FIXED_SPECIFICATIONS::get)
					.reduce(
							FIXED_SPECIFICATIONS.get(interfaces.get(0)),
							(composition, current) -> composition.and(current));
		// @formatter:on
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			SharedSessionContract session) throws Exception {
		return findAll(type, selector, DEFAULT_PAGEABLE, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			LockModeType lockModeType, SharedSessionContract session) throws Exception {
		return findAll(type, selector, DEFAULT_PAGEABLE, lockModeType, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			Pageable pageable, SharedSessionContract session) throws Exception {
		return findAll(type, selector, pageable, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			Pageable pageable, LockModeType lockMode, SharedSessionContract session) throws Exception {
		// @formatter:off
		return declare(criteriaBuilder.createTupleQuery())
				.second(cq -> cq.from(type))
				.third(selector)
			.identical(this::doSelect)
				.useFirstTwo()
				.third(pageable)
			.then(this::doOrder)
				.second(session)
			.then(this::createHQL)
				.second(pageable)
			.then(this::doPaging)
				.second(lockMode)
			.then(this::doLocking)
			.then(Query::list)
			.get();
		// @formatter:on
	}

	private <S extends Serializable, T extends Entity<S>, E> CriteriaQuery<E> doSelect(CriteriaQuery<E> cq,
			Root<T> root, Selector<T, E> selector) {
		return cq.multiselect(selector.select(root, cq, criteriaBuilder));
	}

	private <S extends Serializable, T extends Entity<S>, E> CriteriaQuery<E> doOrder(CriteriaQuery<E> cq, Root<T> root,
			Pageable pageable) throws Exception {
		// @formatter:off
		return declare(pageable.getSort())
				.then(Sort::get)
				.then(stream -> stream.map(order -> toOrder(root, order)))
				.then(Stream::toList)
				.then(cq::orderBy)
				.get();
		// @formatter:on
	}

	private <S extends Serializable, T extends Entity<S>> Order toOrder(Root<T> root,
			org.springframework.data.domain.Sort.Order hbmOrder) {
		if (hbmOrder.isAscending()) {
			return criteriaBuilder.asc(root.get(hbmOrder.getProperty()));
		}

		return criteriaBuilder.desc(root.get(hbmOrder.getProperty()));
	}

	private <E> Query<E> createHQL(CriteriaQuery<E> cq, SharedSessionContract session) {
		return session.createQuery(cq);
	}

	private <E> Query<E> doPaging(Query<E> hql, Pageable pageable) {
		return hql.setMaxResults(pageable.getPageSize())
				.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
	}

	private <E> Query<E> doLocking(Query<E> hql, LockModeType lockMode) {
		return hql.setLockMode(lockMode);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			Specification<T> spec, SharedSessionContract session) throws Exception {
		return findAll(type, selector, spec, DEFAULT_PAGEABLE, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			Specification<T> spec, LockModeType lockModeType, SharedSessionContract session) throws Exception {
		return findAll(type, selector, spec, DEFAULT_PAGEABLE, lockModeType, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			Specification<T> spec, Pageable pageable, SharedSessionContract session) throws Exception {
		return findAll(type, selector, spec, pageable, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<Tuple> findAll(Class<T> type, Selector<T, Tuple> selector,
			Specification<T> spec, Pageable pageable, LockModeType lockMode, SharedSessionContract session)
			throws Exception {
		// @formatter:off
		return declare(criteriaBuilder.createTupleQuery())
				.second(cq -> cq.from(type))
				.third(selector)
			.identical(this::doSelect)
				.useFirstTwo()
				.third(spec)
			.identical(this::doFilter)
				.useFirstTwo()
				.third(pageable)
			.then(this::doOrder)
				.second(session)
			.then(this::createHQL)
				.second(pageable)
			.then(this::doPaging)
				.second(lockMode)
			.then(this::doLocking)
			.then(Query::list)
			.get();
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private <S extends Serializable, T extends Entity<S>, E> CriteriaQuery<E> doFilter(CriteriaQuery<E> cq,
			Root<T> root, Specification<T> requestedSpecication) {
		Specification<T> mandatorySpecification = (Specification<T>) fixedSpecifications
				.get(root.getModel().getJavaType());

		return cq.where(mandatorySpecification.and(requestedSpecication).toPredicate(root, cq, criteriaBuilder));
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, SharedSessionContract session)
			throws Exception {
		return findAll(type, DEFAULT_PAGEABLE, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, LockModeType lockModeType,
			SharedSessionContract session) throws Exception {
		return findAll(type, DEFAULT_PAGEABLE, lockModeType, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, Pageable pageable,
			SharedSessionContract session) throws Exception {
		return findAll(type, pageable, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, Pageable pageable,
			LockModeType lockMode, SharedSessionContract session) throws Exception {
		// @formatter:off
		return declare(criteriaBuilder.createQuery(type))
				.second(cq -> cq.from(type))
				.third(pageable)
			.then(this::doOrder)
				.second(session)
			.then(this::createHQL)
				.second(pageable)
			.then(this::doPaging)
				.second(lockMode)
			.then(this::doLocking)
			.then(Query::list)
			.get();
		// @formatter:on
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, SharedSessionContract session,
			Specification<T> spec) throws Exception {
		return findAll(type, DEFAULT_PAGEABLE, DEFAULT_LOCK_MODE, session, spec);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, LockModeType lockModeType,
			SharedSessionContract session, Specification<T> spec) throws Exception {
		return findAll(type, DEFAULT_PAGEABLE, lockModeType, session, spec);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, Pageable pageable,
			SharedSessionContract session, Specification<T> spec) throws Exception {
		return findAll(type, pageable, DEFAULT_LOCK_MODE, session, spec);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> List<T> findAll(Class<T> type, Pageable pageable,
			LockModeType lockMode, SharedSessionContract session, Specification<T> spec) throws Exception {
		// @formatter:off
		return declare(criteriaBuilder.createQuery(type))
				.second(cq -> cq.from(type))
				.third(spec)
			.identical(this::doFilter)
				.useFirstTwo()
				.third(pageable)
			.then(this::doOrder)
				.second(session)
			.then(this::createHQL)
				.second(pageable)
			.then(this::doPaging)
				.second(lockMode)
			.then(this::doLocking)
			.then(Query::list)
			.get();
		// @formatter:on
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> Optional<T> findById(Class<T> type, Serializable id,
			SharedSessionContract session) throws Exception {
		return findById(type, id, DEFAULT_LOCK_MODE, session);
	}

	private static <T> Specification<T> hasId(Serializable id) {
		return (root, query, builder) -> builder.equal(root.get("id"), id);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> Optional<T> findById(Class<T> type, Serializable id,
			LockModeType lockMode, SharedSessionContract session) throws Exception {
		return findOne(type, hasId(id), lockMode, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> Optional<Tuple> findById(Class<T> type, Serializable id,
			Selector<T, Tuple> selector, SharedSessionContract session) throws Exception {
		return findById(type, id, selector, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> Optional<Tuple> findById(Class<T> type, Serializable id,
			Selector<T, Tuple> selector, LockModeType lockMode, SharedSessionContract session) throws Exception {
		return findOne(type, selector, hasId(id), session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> Optional<T> findOne(Class<T> type, Specification<T> spec,
			SharedSessionContract session) throws Exception {
		return findOne(type, spec, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> Optional<T> findOne(Class<T> type, Specification<T> spec,
			LockModeType lockMode, SharedSessionContract session) throws Exception {
		// @formatter:off
		return declare(criteriaBuilder.createQuery(type))
				.second(cq -> cq.from(type))
				.third(spec)
			.then(this::doFilter)
				.second(session)
			.then(this::createHQL)
				.second(lockMode)
			.then(this::doLocking)
			.then(Query::getResultStream)
			.then(Stream::findFirst)
			.get();
		// @formatter:on
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> Optional<Tuple> findOne(Class<T> type,
			Selector<T, Tuple> selector, Specification<T> spec, SharedSessionContract session) throws Exception {
		return findOne(type, selector, spec, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <S extends Serializable, T extends Entity<S>> Optional<Tuple> findOne(Class<T> type,
			Selector<T, Tuple> selector, Specification<T> spec, LockModeType lockMode, SharedSessionContract session)
			throws Exception {
		// @formatter:off
		return declare(criteriaBuilder.createTupleQuery())
				.second(cq -> cq.from(type))
				.third(selector)
			.identical(this::doSelect)
				.useFirstTwo()
				.third(spec)
			.then(this::doFilter)
				.second(session)
			.then(this::createHQL)
				.second(lockMode)
			.then(this::doLocking)
			.then(Query::getResultStream)
			.then(Stream::findFirst)
			.get();
		// @formatter:on
	}

}
