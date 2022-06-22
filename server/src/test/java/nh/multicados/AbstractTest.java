/**
 * 
 */
package nh.multicados;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.domain.entity.Role;
import multicados.internal.helper.StringHelper;
import multicados.security.userdetails.UserDetailsServiceImpl.DomainUser;
import nh.multicados.internal.service.crud.security.ReadSecurityTests;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractTest {

	private static final Logger logger = LoggerFactory.getLogger(ReadSecurityTests.class);

	private final ObjectMapper objectMapper;

	public static final DomainUser ANONYMOUS = new DomainUser("anonymous_user", "password", true, true,
			LocalDateTime.now(), List.of(new SimpleGrantedAuthority(Role.ANONYMOUS.name())));

	public static final DomainUser HEAD_NGOCHUYOU = new DomainUser("ngochuy.ou", "password", true, true,
			LocalDateTime.now(), List.of(new SimpleGrantedAuthority(Role.HEAD.name())));

	public AbstractTest(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@SuppressWarnings("unchecked")
	protected void logJson(String json, Class<?> containerType)
			throws JsonMappingException, JsonProcessingException, UnsupportedEncodingException {
		if (Collection.class.isAssignableFrom(containerType)) {
			logJsonArray(json, (Class<Collection<?>>) containerType);
			return;
		}

		logJsonObject(json);
	}

	@SuppressWarnings("unchecked")
	private void logJsonArray(String json, Class<Collection<?>> containerType)
			throws JsonMappingException, JsonProcessingException {
		final Collection<Map<String, Object>> elements = (Collection<Map<String, Object>>) objectMapper.readValue(json,
				containerType);

		logger.info("Batch size {}", elements.size());

		for (Map<String, Object> element : elements) {
			logger.info(getObjectString(element, 1));
		}
	}

	private String getIndentation(int indentation) {
		return IntStream.range(0, indentation).mapToObj(index -> "\t").collect(Collectors.joining());
	}

	@SuppressWarnings("unchecked")
	private String getObjectString(Map<String, Object> element, int indentation) {
		final String indentationString = "\n" + getIndentation(indentation);
		final StringBuilder builder = new StringBuilder();

		for (Entry<String, Object> entry : element.entrySet()) {
			Object value = entry.getValue();
			String key = indentationString + entry.getKey() + ":\t";

			if (value == null) {
				builder.append(key + StringHelper.NULL);
				continue;
			}

			if (Map.class.isAssignableFrom(value.getClass())) {
				builder.append(key + getObjectString((Map<String, Object>) value, indentation + 1));
				continue;
			}

			if (Collection.class.isAssignableFrom(value.getClass())) {
				Collection<Map<String, Object>> collection = Collection.class.cast(value);

				for (Map<String, Object> nestedElement : collection) {
					builder.append(key + getObjectString(nestedElement, indentation + 1));
				}

				continue;
			}

			builder.append(key + value.toString());
		}

		return builder.toString();
	}

	@SuppressWarnings("unchecked")
	private void logJsonObject(String json) throws JsonMappingException, JsonProcessingException {
		logger.info(getObjectString(objectMapper.readValue(json, Map.class), 1));
	}

}
