/**
 * 
 */
package multicados.application.context;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.domain.AbstractEntity;
import multicados.internal.context.ContextManager;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.repository.DatabaseInitializer.DatabaseInitializerContributor;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.domain.tuplizer.DomainResourceTuplizer;
import multicados.internal.domain.tuplizer.TuplizerException;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.service.GenericCRUDService;
import multicados.internal.service.ServiceResult;

/**
 * @author Ngoc Huy
 *
 */
@SuppressWarnings("unused")
public class DummyDatabaseInitializer implements DatabaseInitializerContributor {

	@Override
	public void contribute() {
		Session session = HibernateHelper.getSessionFactory().openSession();

		session.setHibernateFlushMode(FlushMode.MANUAL);
		session.beginTransaction();
		// @formatter:off
		try {
//			Utils.declare("data\\dummy\\dummy_categories.json")
//				.then(this::getArray)
//					.second(Category.class)
//				.then(this::toInstances)
//					.second(Category.class)
//					.third(Map.entry(ContextManager.getBean(GenericRepository.class), session))
//				.then(this::filter)
//					.second(Category.class)
//				.identical(this::logInstances)
//					.third(session)
//				.identical(this::create);
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			session.clear();
			session.getTransaction().rollback();
		} finally {
			session.close();
		}
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getArray(String uri) throws StreamReadException, DatabindException, IOException {
		return (List<Map<String, Object>>) ContextManager.getBean(ObjectMapper.class)
				.readValue(new ClassPathResource(uri).getInputStream(), List.class);
	}

	private <S extends Serializable, E extends AbstractEntity<S>> List<E> toInstances(List<Map<String, Object>> objMaps,
			Class<E> entityType) {
		DomainResourceTuplizer<E> tuplizer = ContextManager.getBean(DomainResourceContext.class)
				.getTuplizer(entityType);
		int batchSize = objMaps.size();
		List<E> instances = IntStream.range(0, batchSize).mapToObj(index -> {
			try {
				return tuplizer.instantiate();
			} catch (TuplizerException e) {
				e.printStackTrace();
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toList());

		return IntStream.range(0, batchSize).mapToObj(index -> {
			objMaps.get(index).entrySet().stream().forEach(entry -> {
				try {
					tuplizer.setProperty(instances.get(index), entry.getKey(), entry.getValue());
				} catch (TuplizerException e) {
					e.printStackTrace();
				}
			});

			return instances.get(index);
		}).collect(Collectors.toList());
	}

	private <S extends Serializable, E extends AbstractEntity<S>> List<E> filter(List<E> instances, Class<E> entityType,
			Map.Entry<GenericRepository, Session> argEntry) throws Exception {
		GenericRepository repository = argEntry.getKey();
		Session session = argEntry.getValue();
		List<Tuple> tuples = repository.findAll(entityType,
				(root, query, builder) -> List.of(root.get(AbstractEntity.id_), root.get(NamedResource.name_)),
				(root, query, builder) -> builder.in(root.get(NamedResource.name_)).value(root),
				LockModeType.PESSIMISTIC_WRITE, session);

		return instances;
	}

	private <S extends Serializable, E extends AbstractEntity<S>> void create(List<E> instances, Class<E> entityType,
			Session entityManager) throws Exception {
		final Logger logger = LoggerFactory.getLogger(DummyDatabaseInitializer.class);
		GenericCRUDService crudService = ContextManager.getBean(GenericCRUDService.class);

		for (E e : instances) {
			ServiceResult result = crudService.create(e.getId(), e, entityType, entityManager, true);

			if (result.isOk()) {
				continue;
			}

			Exception exception = result.getException();

			if (exception != null) {
				throw exception;
			}

			logger.error("Failed to create dummy resource of type {}", entityType.getName());
		}
	}

	private <S extends Serializable, E extends AbstractEntity<S>> void logInstances(List<E> instances, Class<E> entityType) {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		if (!logger.isTraceEnabled()) {
			return;
		}

		DomainResourceTuplizer<E> tuplizer = ContextManager.getBean(DomainResourceContext.class)
				.getTuplizer(entityType);
		DomainResourceMetadata<E> metadata = ContextManager.getBean(DomainResourceContext.class)
				.getMetadata(entityType);

		instances.stream().forEach(instance -> {
			logger.trace("{}({})", entityType.getName(), metadata.getAttributeNames().stream().map(propName -> {
				try {
					return String.format("%s: %s", propName, tuplizer.getProperty(instance, propName));
				} catch (TuplizerException e) {
					e.printStackTrace();
					return StringHelper.EMPTY_STRING;
				}
			}).collect(Collectors.joining(", ")));
		});
	}

}
