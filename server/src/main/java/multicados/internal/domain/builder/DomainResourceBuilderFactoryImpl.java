/**
 * 
 */
package multicados.internal.domain.builder;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.SharedSessionContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Constants;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.Entity;
import multicados.internal.domain.For;
import multicados.internal.helper.FunctionHelper.HandledFunction;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceBuilderFactoryImpl implements DomainResourceBuilderFactory {

	@SuppressWarnings("rawtypes")
	private final Map<Class, DomainResourceBuilder> buildersMap;

	public DomainResourceBuilderFactoryImpl() throws Exception {
		// @formatter:off
		this.buildersMap = Utils.declare(scan())
				.then(this::doFirstPhaseRegister)
				.then(this::doSecondPhaseRegister)
				.then(Collections::unmodifiableMap)
				.get();
		// @formatter:on
	}

	private Set<BeanDefinition> scan() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		final Logger logger = LoggerFactory.getLogger(DomainResourceBuilderFactoryImpl.class);

		scanner.addIncludeFilter(new AssignableTypeFilter(DomainResourceBuilder.class));

		Stream.of(EntityBuilder.class).forEach(clazz -> scanner.addExcludeFilter(new AssignableTypeFilter(clazz)));

		logger.trace("Scanning for {}", DomainResourceBuilder.class.getSimpleName());

		return scanner.findCandidateComponents(Constants.BASE_PACKAGE);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<Class, DomainResourceBuilder> doFirstPhaseRegister(Set<BeanDefinition> beanDefs)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		Map<Class, DomainResourceBuilder> buildersMap = new HashMap<>(8);

		for (BeanDefinition beanDef : beanDefs) {
			Class<DomainResourceBuilder> builderClass = (Class<DomainResourceBuilder>) Class
					.forName(beanDef.getBeanClassName());
			For anno = builderClass.getDeclaredAnnotation(For.class);

			if (anno == null) {
				throw new IllegalArgumentException(For.MISSING_MESSAGE);
			}

			buildersMap.put(anno.value(), TypeHelper.constructFromNonArgs(builderClass));
		}

		return buildersMap;
	}

	@SuppressWarnings("rawtypes")
	private Map<Class, DomainResourceBuilder> doSecondPhaseRegister(Map<Class, DomainResourceBuilder> builderMap) {
		builderMap.put(Entity.class, new EntityBuilder());

		return builderMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends DomainResource, T extends DomainResourceBuilder<E>> DomainResourceBuilder<E> getBuilder(
			Class<E> resourceClass) {
		DomainResourceBuilder<E> domainResourceBuilder = (DomainResourceBuilder<E>) buildersMap.get(resourceClass);

		return domainResourceBuilder;
	}

	@SuppressWarnings("rawtypes")
	@For(Entity.class)
	private static class EntityBuilder extends AbstractDomainResourceBuilder<Entity> {

		private EntityBuilder() {}

		private static final Map<Class<? extends Serializable>, Class<? extends Serializable>> TYPE_KEY_RESOLVERS = Map
				.of(String.class, String.class);

		private static final Map<Class<? extends Serializable>, HandledFunction<Serializable, Serializable, Exception>> HANDLER_RESOLVERS;

		static {
			Map<Class<? extends Serializable>, HandledFunction<Serializable, Serializable, Exception>> handlerResolvers = new HashMap<>(
					8);

			handlerResolvers.put(String.class, id -> StringHelper.normalizeString((String) id));
			handlerResolvers.put(null, id -> id);

			HANDLER_RESOLVERS = Collections.unmodifiableMap(handlerResolvers);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <E extends Entity> E buildInsertion(Serializable id, E resource, SharedSessionContract session)
				throws Exception {
			// @formatter:off
			id = Utils.declare(id.getClass())
					.then(TYPE_KEY_RESOLVERS::get)
					.then(HANDLER_RESOLVERS::get)
					.get()
					.apply(id);
			// @formatter:on
			resource.setId(id);

			return resource;
		}

		@Override
		public <E extends Entity> E buildUpdate(Serializable id, E model, E resource, SharedSessionContract session) {
			return resource;
		}

		@Override
		public String getLoggableName() {
			return EntityBuilder.class.getSimpleName();
		}

	}

}
