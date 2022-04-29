/**
 * 
 */
package multicados.application.data;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.Session;
import org.hibernate.tuple.IdentifierProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.domain.AbstractEntity;
import multicados.internal.config.Settings;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.metadata.AssociationType;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.tuplizer.DomainResourceTuplizer;
import multicados.internal.domain.tuplizer.TuplizerException;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;
import multicados.internal.service.ServiceResult;
import multicados.internal.service.crud.GenericCRUDServiceImpl;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractDummyDatabaseContributor {

	private final DomainResourceContext resourceContext;
	private final GenericCRUDServiceImpl crudService;
	private final ObjectMapper objectMapper;

	private final String path;

	public AbstractDummyDatabaseContributor(ObjectMapper objectMapper, DomainResourceContext resourceContext,
			GenericCRUDServiceImpl crudService, Environment env) {
		super();
		this.resourceContext = resourceContext;
		this.crudService = crudService;
		this.objectMapper = objectMapper;
		path = Optional.of(env.getProperty(Settings.DUMMY_DATABASE_PATH)).get();
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getArray(String uri) throws StreamReadException, DatabindException, IOException {
		return (List<Map<String, Object>>) objectMapper.readValue(new ClassPathResource(uri).getInputStream(),
				List.class);
	}

	@SuppressWarnings("unchecked")
	private Object checkType(Class<?> expecting, Object value) throws Exception {
		if (expecting.isAssignableFrom(value.getClass())) {
			return value;
		}

		return TypeHelper.cast((Class<Object>) value.getClass(), (Class<Object>) expecting, value);
	}

	private <E extends DomainResource> List<E> toInstances(List<Map<String, Object>> objMaps, Class<E> type)
			throws Exception {
		DomainResourceTuplizer<E> tuplizer = resourceContext.getTuplizer(type);
		int batchSize = objMaps.size();
		List<E> instances = IntStream.range(0, batchSize).mapToObj(index -> {
			try {
				return tuplizer.instantiate();
			} catch (TuplizerException e) {
				e.printStackTrace();
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toList());
		DomainResourceMetadata<E> metadata = resourceContext.getMetadata(type);

		return IntStream.range(0, batchSize).mapToObj(index -> {
			Map<String, Object> state = objMaps.get(index);

			for (Map.Entry<String, Object> entry : state.entrySet()) {
				try {
					String attributeName = entry.getKey();
					Object value = resolveValue(attributeName, entry.getValue(), metadata);

					tuplizer.setProperty(instances.get(index), attributeName, value);
				} catch (Exception any) {
					any.printStackTrace();
				}
			}

			return instances.get(index);
		}).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private Object resolveValue(String attributeName, Object value,
			DomainResourceMetadata<? extends DomainResource> metadata) throws Exception {
		if (metadata.isAssociation(attributeName)
				&& metadata.getAssociationType(attributeName) == AssociationType.ENTITY) {
			Class<? extends DomainResource> associationJavaType = (Class<? extends DomainResource>) metadata
					.getAttributeType(attributeName);
			DomainResourceTuplizer<? extends DomainResource> associationTuplizer = resourceContext
					.getTuplizer((Class<? extends DomainResource>) associationJavaType);
			Object associationValue = associationTuplizer.instantiate();
			IdentifierProperty identifierProperty = HibernateHelper.getEntityPersister(associationJavaType)
					.getEntityMetamodel().getIdentifierProperty();

			associationTuplizer.setProperty(associationValue, identifierProperty.getName(),
					checkType((Class<Object>) identifierProperty.getType().getReturnedClass(), value));
			return associationValue;
		}

		if (!metadata.getAttributeType(attributeName).equals(value.getClass())) {
			return checkType((Class<Object>) metadata.getAttributeType(attributeName), value);
		}

		return value;
	}

	private String getPath(String filename) {
		return String.format("%s%s", path, filename);
	}

	protected <S extends Serializable, E extends AbstractEntity<S>> void createBatch(Class<E> type, String path,
			Session entityManager, Consumer<Exception> exceptionConsumer) throws Exception {
		// @formatter:off
		List<E> instances = Utils.declare(getPath(path))
				.then(this::getArray)
					.second(type)
				.then(this::toInstances)
				.get();

		for (E e : instances) {
			ServiceResult result = crudService.create(e.getId(), e, type, entityManager, true);

			if (result.isOk()) {
				continue;
			}

			exceptionConsumer.accept(result.getException());
		}
		// @formatter:on
	}

}
