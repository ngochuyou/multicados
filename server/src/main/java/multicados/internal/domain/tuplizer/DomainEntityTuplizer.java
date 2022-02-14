/**
 * 
 */
package multicados.internal.domain.tuplizer;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.util.Assert;

import multicados.domain.entity.Entity;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.tuplizer.AccessorFactory.Accessor;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class DomainEntityTuplizer<I extends Serializable, E extends Entity<I>>
		extends AbstractDomainResourceTuplizer<E> {

	private static final String NULL_ACCESSOR_TEMPLATE = String.format("Unable to locate %s with property name [%s]",
			Accessor.class.getSimpleName(), "%s");

	private final Map<String, Accessor> accessors;

	public DomainEntityTuplizer(Class<E> resourceType, DomainResourceContext resourceContext,
			SessionFactoryImplementor sfi) throws Exception {
		super(resourceType);
		// @formatter:off
		this.accessors = Utils.declare(resourceContext.getMetadata(resourceType).getAttributeNames())
				.second(sfi)
				.third(resourceType)
				.triInverse()
				.then(this::mapToAccessors)
				.then(Collections::unmodifiableMap)
				.get();
		// @formatter:on
	}

	private Map<String, Accessor> mapToAccessors(Class<E> resourceType, SessionFactoryImplementor sfi,
			Collection<String> propertyNames) {
		return propertyNames.stream()
				.map(propName -> Map.entry(propName, AccessorFactory.hbm(resourceType, propName, sfi)))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	protected Getter getGetter(String propName) {
		Accessor accessor = accessors.get(propName);

		Assert.notNull(accessor, String.format(NULL_ACCESSOR_TEMPLATE, propName));

		return accessor.getGetter();
	}

	@Override
	protected Setter getSetter(String propName) {
		Accessor accessor = accessors.get(propName);

		Assert.notNull(accessor, String.format(NULL_ACCESSOR_TEMPLATE, propName));

		return accessor.getSetter();
	}

}
