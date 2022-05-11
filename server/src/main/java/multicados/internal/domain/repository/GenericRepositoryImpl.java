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

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraph;
import multicados.internal.domain.DomainResourceGraphCollectors;
import multicados.internal.domain.PermanentResource;
import multicados.internal.helper.SpecificationHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class GenericRepositoryImpl implements GenericRepository {

	private static final Logger logger = LoggerFactory.getLogger(GenericRepositoryImpl.class);

	private static final Pageable DEFAULT_PAGEABLE = Pageable.ofSize(10);
	private static final Pageable SINGLE_ROW_PAGEABLE = Pageable.ofSize(1);
	private static final LockModeType DEFAULT_LOCK_MODE = LockModeType.NONE;

	private final Map<Class<? extends DomainResource>, Specification<? extends DomainResource>> fixedSpecifications;

	private final CriteriaBuilder criteriaBuilder;

	@SuppressWarnings({ "unchecked" })
	public GenericRepositoryImpl(DomainResourceContext resourceContextProvider, SessionFactoryImplementor sfi)
			throws Exception {
		Map<Class<? extends DomainResource>, Specification<? extends DomainResource>> fixedSpecifications = new HashMap<>(
				0);

		for (DomainResourceGraph<DomainResource> node : resourceContextProvider.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toGraphsSet())) {
			Class<? extends DomainResource> entityType = (Class<? extends DomainResource>) node.getResourceType();

			if (Modifier.isAbstract(entityType.getModifiers())) {
				logger.trace("Skipping abstract {} type {}", DomainResource.class.getSimpleName(),
						entityType.getName());
				continue;
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

	private <D extends DomainResource> Set<Class<?>> getAllInterfaces(Class<D> entityType) {
		logger.trace("Finding all interfaces of {} type [{}]", DomainResource.class.getSimpleName(),
				entityType.getSimpleName());

		return ClassUtils.getAllInterfacesForClassAsSet(entityType);
	}

	private Set<Class<?>> filterOutNonTargetedTypes(Set<Class<?>> interfaces) {
		logger.trace("Filtering fixed interfaces");
		return interfaces.stream().filter(FIXED_SPECIFICATIONS::containsKey).collect(Collectors.toSet());
	}

	@SuppressWarnings({ "rawtypes" })
	private static final Map<Class, Specification> FIXED_SPECIFICATIONS = Map.of(PermanentResource.class,
			(root, query, builder) -> builder.equal(root.get(PermanentResource.ACTIVE), Boolean.TRUE));

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Specification chainFixedSpecifications(List<Class<?>> interfaces) {
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

	private void log(Query<?> query) {
		if (logger.isDebugEnabled()) {
			logger.debug(query.getQueryString());
		}
	}

	@Override
	public <D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector,
			SharedSessionContract session) throws Exception {
		return findAll(type, selector, DEFAULT_PAGEABLE, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector,
			LockModeType lockModeType, SharedSessionContract session) throws Exception {
		return findAll(type, selector, DEFAULT_PAGEABLE, lockModeType, session);
	}

	@Override
	public <D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector, Pageable pageable,
			SharedSessionContract session) throws Exception {
		return findAll(type, selector, pageable, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector, Pageable pageable,
			LockModeType lockMode, SharedSessionContract session) throws Exception {
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
			.identical(this::log)
			.then(Query::list)
			.get();
		// @formatter:on
	}

	private <D extends DomainResource, E> CriteriaQuery<E> doSelect(CriteriaQuery<E> cq, Root<D> root,
			Selector<D, E> selector) {
		return cq.multiselect(selector.select(root, cq, criteriaBuilder));
	}

	private <D extends DomainResource, E> CriteriaQuery<E> doOrder(CriteriaQuery<E> cq, Root<D> root, Pageable pageable)
			throws Exception {
		// @formatter:off
		return declare(pageable.getSort())
				.then(Sort::get)
				.then(stream -> stream.map(order -> toOrder(root, order)))
				.then(Stream::toList)
				.then(cq::orderBy)
				.get();
		// @formatter:on
	}

	private <D extends DomainResource> Order toOrder(Root<D> root,
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
	public <D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector,
			Specification<D> spec, SharedSessionContract session) throws Exception {
		return findAll(type, selector, spec, DEFAULT_PAGEABLE, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector,
			Specification<D> spec, LockModeType lockModeType, SharedSessionContract session) throws Exception {
		return findAll(type, selector, spec, DEFAULT_PAGEABLE, lockModeType, session);
	}

	@Override
	public <D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector,
			Specification<D> spec, Pageable pageable, SharedSessionContract session) throws Exception {
		return findAll(type, selector, spec, pageable, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <D extends DomainResource> List<Tuple> findAll(Class<D> type, Selector<D, Tuple> selector,
			Specification<D> specification, Pageable pageable, LockModeType lockMode, SharedSessionContract session)
			throws Exception {
		// @formatter:off
		return declare(criteriaBuilder.createTupleQuery())
				.second(cq -> cq.from(type))
				.third(selector)
			.identical(this::doSelect)
				.useFirstTwo()
				.third(specification)
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
			.identical(this::log)
			.then(Query::list)
			.get();
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private <D extends DomainResource, E> CriteriaQuery<E> doFilter(CriteriaQuery<E> cq, Root<D> root,
			Specification<D> requestedSpecication) {
		Specification<D> mandatorySpecification = (Specification<D>) fixedSpecifications
				.get(root.getModel().getJavaType());

		return cq.where(mandatorySpecification.and(requestedSpecication).toPredicate(root, cq, criteriaBuilder));
	}

	@Override
	public <D extends DomainResource> List<D> findAll(Class<D> type, SharedSessionContract session) throws Exception {
		return findAll(type, DEFAULT_PAGEABLE, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <D extends DomainResource> List<D> findAll(Class<D> type, LockModeType lockModeType,
			SharedSessionContract session) throws Exception {
		return findAll(type, DEFAULT_PAGEABLE, lockModeType, session);
	}

	@Override
	public <D extends DomainResource> List<D> findAll(Class<D> type, Pageable pageable, SharedSessionContract session)
			throws Exception {
		return findAll(type, pageable, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <D extends DomainResource> List<D> findAll(Class<D> type, Pageable pageable, LockModeType lockMode,
			SharedSessionContract session) throws Exception {
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
			.identical(this::log)
			.then(Query::list)
			.get();
		// @formatter:on
	}

	@Override
	public <D extends DomainResource> List<D> findAll(Class<D> type, SharedSessionContract session,
			Specification<D> spec) throws Exception {
		return findAll(type, DEFAULT_PAGEABLE, DEFAULT_LOCK_MODE, session, spec);
	}

	@Override
	public <D extends DomainResource> List<D> findAll(Class<D> type, LockModeType lockModeType,
			SharedSessionContract session, Specification<D> spec) throws Exception {
		return findAll(type, DEFAULT_PAGEABLE, lockModeType, session, spec);
	}

	@Override
	public <D extends DomainResource> List<D> findAll(Class<D> type, Pageable pageable, SharedSessionContract session,
			Specification<D> spec) throws Exception {
		return findAll(type, pageable, DEFAULT_LOCK_MODE, session, spec);
	}

	@Override
	public <D extends DomainResource> List<D> findAll(Class<D> type, Pageable pageable, LockModeType lockMode,
			SharedSessionContract session, Specification<D> spec) throws Exception {
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
			.identical(this::log)
			.then(Query::list)
			.get();
		// @formatter:on
	}

	@Override
	public <D extends DomainResource> Optional<D> findById(Class<D> type, Serializable id,
			SharedSessionContract session) throws Exception {
		return findById(type, id, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <D extends DomainResource> Optional<D> findById(Class<D> type, Serializable id, LockModeType lockMode,
			SharedSessionContract session) throws Exception {
		return findOne(type, SpecificationHelper.hasId(type, id), lockMode, session);
	}

	@Override
	public <D extends DomainResource> Optional<Tuple> findById(Class<D> type, Serializable id,
			Selector<D, Tuple> selector, SharedSessionContract session) throws Exception {
		return findById(type, id, selector, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <D extends DomainResource> Optional<Tuple> findById(Class<D> type, Serializable id,
			Selector<D, Tuple> selector, LockModeType lockMode, SharedSessionContract session) throws Exception {
		return findOne(type, selector, SpecificationHelper.hasId(type, id), session);
	}

	@Override
	public <D extends DomainResource> Optional<D> findOne(Class<D> type, Specification<D> spec,
			SharedSessionContract session) throws Exception {
		return findOne(type, spec, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <D extends DomainResource> Optional<D> findOne(Class<D> type, Specification<D> spec, LockModeType lockMode,
			SharedSessionContract session) throws Exception {
		// @formatter:off
		return declare(criteriaBuilder.createQuery(type))
				.second(cq -> cq.from(type))
				.third(spec)
			.then(this::doFilter)
				.second(session)
			.then(this::createHQL)
				.second(SINGLE_ROW_PAGEABLE)
			.then(this::doPaging)
				.second(lockMode)
			.then(this::doLocking)
			.identical(this::log)
			.then(Query::getResultStream)
			.then(Stream::findFirst)
			.get();
		// @formatter:on
	}

	@Override
	public <D extends DomainResource> Optional<Tuple> findOne(Class<D> type, Selector<D, Tuple> selector,
			Specification<D> spec, SharedSessionContract session) throws Exception {
		return findOne(type, selector, spec, DEFAULT_LOCK_MODE, session);
	}

	@Override
	public <D extends DomainResource> Optional<Tuple> findOne(Class<D> type, Selector<D, Tuple> selector,
			Specification<D> spec, LockModeType lockMode, SharedSessionContract session) throws Exception {
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
				.second(SINGLE_ROW_PAGEABLE)
			.then(this::doPaging)
				.second(lockMode)
				.then(this::doLocking)
			.identical(this::log)
			.then(Query::getResultStream)
			.then(Stream::findFirst)
			.get();
		// @formatter:on
	}

}
