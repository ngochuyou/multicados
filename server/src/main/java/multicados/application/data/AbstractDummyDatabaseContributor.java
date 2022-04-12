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
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.domain.AbstractEntity;
import multicados.internal.config.Settings;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.tuplizer.DomainResourceTuplizer;
import multicados.internal.domain.tuplizer.TuplizerException;
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

	private <E extends DomainResource> List<E> toInstances(List<Map<String, Object>> objMaps, Class<E> entityType) {
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
			Map<String, Object> state = objMaps.get(index);

			for (Map.Entry<String, Object> entry : state.entrySet()) {
				try {
					tuplizer.setProperty(instances.get(index), entry.getKey(), entry.getValue());
				} catch (TuplizerException e) {
					e.printStackTrace();
				}
			}

			return instances.get(index);
		}).collect(Collectors.toList());
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
