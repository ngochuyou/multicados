/**
 * 
 */
package multicados.application.context;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.internal.context.ContextManager;
import multicados.internal.domain.repository.DatabaseInitializer.DatabaseInitializerContributor;

/**
 * @author Ngoc Huy
 *
 */
public class DummyDatabaseInitializer implements DatabaseInitializerContributor {

	@Override
	public void contribute() throws StreamReadException, DatabindException, IOException {
		getArray("data\\dummy\\dummy_categories.json");
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getArray(String uri) throws StreamReadException, DatabindException, IOException {
		return (List<Map<String, Object>>) ContextManager.getBean(ObjectMapper.class)
				.readValue(new ClassPathResource(uri).getInputStream(), List.class);
	}

}
