/**
 * 
 */
package multicados.application.data;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.domain.AbstractEntity;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.tuplizer.DomainResourceTuplizer;
import multicados.internal.domain.tuplizer.TuplizerException;
import multicados.internal.helper.StringHelper;
import multicados.internal.service.GenericCRUDService;
import multicados.internal.service.ServiceResult;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractDummyDatabaseContributor {

	private final DomainResourceContext resourceContext;
	private final GenericCRUDService crudService;
	private final ObjectMapper objectMapper;

	public AbstractDummyDatabaseContributor(ObjectMapper objectMapper, DomainResourceContext resourceContext,
			GenericCRUDService crudService) {
		super();
		this.resourceContext = resourceContext;
		this.crudService = crudService;
		this.objectMapper = objectMapper;
	}

	@SuppressWarnings("unchecked")
	protected List<Map<String, Object>> getArray(String uri)
			throws StreamReadException, DatabindException, IOException {
		return (List<Map<String, Object>>) objectMapper.readValue(new ClassPathResource(uri).getInputStream(),
				List.class);
	}

	protected <S extends Serializable, E extends AbstractEntity<S>> List<E> toInstances(
			List<Map<String, Object>> objMaps, Class<E> entityType) {

		DomainResourceTuplizer<E> tuplizer = resourceContext.getTuplizer(entityType);
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

	protected <S extends Serializable, E extends AbstractEntity<S>> void logInstances(List<E> instances,
			Class<E> entityType) {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

		if (!logger.isTraceEnabled()) {
			return;
		}

		DomainResourceTuplizer<E> tuplizer = resourceContext.getTuplizer(entityType);
		DomainResourceMetadata<E> metadata = resourceContext.getMetadata(entityType);

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

	protected <S extends Serializable, E extends AbstractEntity<S>> void create(List<E> instances, Class<E> entityType,
			Session entityManager) throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());

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

}
