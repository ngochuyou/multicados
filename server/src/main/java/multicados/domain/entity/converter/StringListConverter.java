/**
 * 
 */
package multicados.domain.entity.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

	private static final String DELIMETER = ",";

	@Override
	public String convertToDatabaseColumn(List<String> attributes) {
		return CollectionHelper.isEmpty(attributes) ? null
				: attributes.stream().collect(Collectors.joining(StringHelper.COMMA));
	}

	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		return dbData != null ? List.of(dbData.split(DELIMETER)) : new ArrayList<>();
	}

}