/**
 *
 */
package multicados.internal.config;

import static java.util.Map.entry;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import multicados.internal.context.ContextBuilder;
import multicados.internal.context.DomainLogicUtils;
import multicados.internal.context.DomainLogicUtilsImpl;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceContextImpl;
import multicados.internal.domain.builder.DomainResourceBuilderFactory;
import multicados.internal.domain.builder.DomainResourceBuilderFactoryImpl;
import multicados.internal.domain.repository.DatabaseInitializer;
import multicados.internal.domain.repository.DatabaseInitializerImpl;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.domain.repository.GenericRepositoryImpl;
import multicados.internal.domain.validation.DomainResourceValidatorFactory;
import multicados.internal.domain.validation.DomainResourceValidatorFactoryImpl;
import multicados.internal.file.engine.FileManagement;
import multicados.internal.file.engine.FileManagementImpl;
import multicados.internal.service.crud.GenericCRUDService;
import multicados.internal.service.crud.GenericCRUDServiceImpl;
import multicados.internal.service.crud.security.read.ReadSecurityManager;
import multicados.internal.service.crud.security.read.ReadSecurityManagerImpl;

/**
 * @author Ngoc Huy
 *
 */
@Configuration
public class DomainLogicContextConfiguration implements ImportBeanDefinitionRegistrar {

	// @formatter:off
	private final List<Map.Entry<Class<? extends ContextBuilder>, Class<? extends ContextBuilder>>> contextBuilderEntries = List.of(
			entry(DomainLogicUtils.class, DomainLogicUtilsImpl.class),
			entry(FileManagement.class, FileManagementImpl.class),
			entry(DomainResourceContext.class, DomainResourceContextImpl.class),
			entry(GenericRepository.class, GenericRepositoryImpl.class),
			entry(DomainResourceValidatorFactory.class, DomainResourceValidatorFactoryImpl.class),
			entry(DomainResourceBuilderFactory.class, DomainResourceBuilderFactoryImpl.class),
			entry(ReadSecurityManager.class, ReadSecurityManagerImpl.class),
			entry(GenericCRUDService.class, GenericCRUDServiceImpl.class),
			entry(DatabaseInitializer.class, DatabaseInitializerImpl.class));
	// @formatter:on
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		for (Entry<Class<? extends ContextBuilder>, Class<? extends ContextBuilder>> entry : contextBuilderEntries) {
			final String beanIdentifier = getBeanIdentifier(entry.getKey());
			final Class<? extends ContextBuilder> beanClass = entry.getValue();
			final GenericBeanDefinition beanDef = new GenericBeanDefinition();

			beanDef.setBeanClass(beanClass);
			beanDef.setScope(BeanDefinition.SCOPE_SINGLETON);
			beanDef.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

			registry.registerBeanDefinition(beanIdentifier, beanDef);
		}
	}

	private String getBeanIdentifier(Class<? extends ContextBuilder> type) {
		return type.getName();
	}

}
