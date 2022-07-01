/**
 * 
 */
package multicados.internal.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Settings;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.annotation.Name;
import multicados.internal.helper.Utils.HandledFunction;

/**
 * @author Ngoc Huy
 *
 */
public class DomainLogicUtilsImpl implements DomainLogicUtils {

	private final Map<Class<? extends DomainResource>, SpecificLogicScopingMetadata<? extends DomainResource>> metadataProviders;

	public DomainLogicUtilsImpl() throws Exception {
		metadataProviders = Collections.unmodifiableMap(constructMetadataProviders());
	}

	private Map<Class<? extends DomainResource>, SpecificLogicScopingMetadata<? extends DomainResource>> constructMetadataProviders()
			throws Exception {
		final Map<Class<? extends DomainResource>, SpecificLogicScopingMetadata<? extends DomainResource>> providers = new HashMap<>();

		providers.putAll(constructSpecificLogicScopingMetadatas(NamedResource.class, Name.class,
				type -> Name.Message.getMissingMessage(type)));

		return providers;
	}

	@SuppressWarnings("unchecked")
	private <D extends DomainResource> Map<Class<? extends D>, SpecificLogicScopingMetadata<? extends D>> constructSpecificLogicScopingMetadatas(
			Class<D> resourceType, Class<? extends Annotation> annotaionType,
			Function<Class<? extends D>, String> missingMessageProducer) throws Exception {
		final Map<Class<? extends D>, SpecificLogicScopingMetadata<? extends D>> metadataProviders = new HashMap<>();
		final Set<Class<? extends D>> scannedResourceTypes = scan(resourceType,
				(beanDef) -> (Class<? extends D>) Class.forName(beanDef.getBeanClassName()));

		for (final Class<? extends D> type : scannedResourceTypes) {
			metadataProviders.put(type, new MetadataProviderImpl<>(
					List.of(resolveAttributeNameWithAnnotation(type, annotaionType, missingMessageProducer))));
		}

		return metadataProviders;
	}

	private <D extends DomainResource> Field resolveAttributeNameWithAnnotation(final Class<? extends D> type,
			Class<? extends Annotation> annotaionType, Function<Class<? extends D>, String> missingMessageProducer) {
		for (final Field field : type.getDeclaredFields()) {
			final Annotation anno = field.getDeclaredAnnotation(annotaionType);

			if (anno != null) {
				return field;
			}
		}

		throw new IllegalArgumentException(missingMessageProducer.apply(type));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <D extends DomainResource> SpecificLogicScopingMetadata<D> getScopingMetadata(Class<D> resourceType) {
		return (SpecificLogicScopingMetadata<D>) metadataProviders.get(resourceType);
	}

	private <D extends DomainResource, E> Set<E> scan(Class<D> resourceType,
			HandledFunction<BeanDefinition, E, Exception> mapper) throws Exception {
		final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
				false);

		scanner.addIncludeFilter(new AssignableTypeFilter(resourceType));

		final Set<E> result = new HashSet<>();

		for (final BeanDefinition beanDefinition : scanner.findCandidateComponents(Settings.BASE_PACKAGE)) {
			result.add(mapper.apply(beanDefinition));
		}

		return result;
	}

}
