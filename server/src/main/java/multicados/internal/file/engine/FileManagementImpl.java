/**
 * 
 */
package multicados.internal.file.engine;

import static multicados.internal.helper.Utils.declare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.SharedCacheMode;

import org.hibernate.EntityMode;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.boot.AttributeConverterInfo;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.IdGeneratorInterpreterImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.boot.spi.BasicTypeRegistration;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.cfg.annotations.reflection.internal.JPAXMLOverriddenMetadataProvider;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.config.internal.ConfigurationServiceImpl;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.property.access.internal.PropertyAccessStrategyFieldImpl;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.service.internal.SessionFactoryServiceRegistryImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;
import org.jboss.jandex.IndexView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextBuilder;
import multicados.internal.file.domain.FileResource;
import multicados.internal.file.engine.image.ImageService;
import multicados.internal.file.engine.image.ManipulationContextImpl;
import multicados.internal.helper.SpringHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils.HandledFunction;
import multicados.internal.locale.ZoneContext;

/**
 * @author Ngoc Huy
 *
 */
public class FileManagementImpl extends ContextBuilder.AbstractContextBuilder implements FileManagement {

	private static final Logger logger = LoggerFactory.getLogger(FileManagementImpl.class);

	private final FileResourceSessionFactory sessionFactory;

	@Autowired
	public FileManagementImpl(ApplicationContext applicationContext, Environment env, SessionFactoryImplementor sfi)
			throws Exception {
		// @formatter:off
		logger.info("\n\n"
				+ "\t\t\t\t\t\t========================================================\n"
				+ "\t\t\t\t\t\t=          BUILDING FILE RESOURCE MANAGEMENT           =\n"
				+ "\t\t\t\t\t\t========================================================\n");
		// @formatter:on
		BootstrapServiceRegistry bootstrapServiceRegistry = createBootstrapServiceRegistry(sfi);
		StandardServiceRegistry standardServiceRegistry = createStandardServiceRegistry(sfi, bootstrapServiceRegistry,
				new ProvidedServicesLocator(applicationContext, sfi, env));
		MetadataBuildingOptions metadataBuildingOptions = new MetadataBuildingOptionsImpl(standardServiceRegistry);
		BootstrapContext bootstrapContext = new BootstrapContextImpl(standardServiceRegistry, metadataBuildingOptions);

		declare(metadataBuildingOptions).then(MetadataBuildingOptionsImpl.class::cast)
				.consume(self -> self.makeReflectionManager(bootstrapContext));

		sessionFactory = build(env, sfi, standardServiceRegistry, bootstrapContext, metadataBuildingOptions);
	}

	// @formatter:off
	private FileResourceSessionFactory build(
			Environment env,
			SessionFactoryImplementor sfi,
			StandardServiceRegistry serviceRegistry,
			BootstrapContext bootstrapContext,
			MetadataBuildingOptions metadataBuildingOptions) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Building {}", FileResourceSessionFactory.class.getName());
		}
		
		return declare(serviceRegistry, env)
				.then(this::scanForMetadataSources)
					.second(bootstrapContext)
					.third(metadataBuildingOptions)
				.then(MetadataBuildingProcess::build)
					.second(getSessionFactoryOptionsBuilder(sfi, serviceRegistry, bootstrapContext))
					.<QueryPlanCache.QueryPlanCreator>third(HQLQueryPlan::new)
				.then((metadataImplementor, factoryOptions, planCreator) -> new FileResourceSessionFactoryImpl(env, metadataImplementor, factoryOptions, planCreator))
				.get();
	}

	private SessionFactoryOptionsBuilder getSessionFactoryOptionsBuilder(SessionFactoryImplementor sfi,
			StandardServiceRegistry serviceRegistry, BootstrapContext bootstrapContext) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating {}", SessionFactoryOptionsBuilder.class.getName());
		}
		// @formatter:off
		return declare(serviceRegistry, bootstrapContext)
				.then(SessionFactoryOptionsBuilder::new)
				.consume(options -> options.applyCurrentTenantIdentifierResolver(sfi.getCurrentTenantIdentifierResolver()))
				.get();
		// @formatter:on
	}

	// @formatter:on
	private MetadataSources scanForMetadataSources(StandardServiceRegistry serviceRegistry, Environment env)
			throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Scanning for {}", MetadataSources.class.getName());
		}

		MetadataSources metadataSources = new MetadataSources(serviceRegistry, true);
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

		scanner.addIncludeFilter(new AssignableTypeFilter(FileResource.class));

		for (BeanDefinition beanDefinition : scanner.findCandidateComponents(SpringHelper.getOrDefault(env,
				Settings.SCANNED_FILE_RESOURCE_PACKAGE, HandledFunction.identity(), Settings.BASE_PACKAGE))) {
			Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());

			metadataSources.addAnnotatedClass(clazz);
			metadataSources.addAnnotatedClassName(clazz.getName());
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Found {} annotated classes", metadataSources.getAnnotatedClasses().size());
		}

		return metadataSources;
	}

	private StandardServiceRegistry createStandardServiceRegistry(SessionFactoryImplementor sfi,
			BootstrapServiceRegistry bootstrapServiceRegistry, ProvidedServicesLocator providedServicesLocator)
			throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating {} ", StandardServiceRegistry.class.getName());
		}
		// @formatter:off
		return new StandardServiceRegistryImpl(
				bootstrapServiceRegistry,
				Collections.emptyList(),
				providedServicesLocator.providedServices,
				Collections.emptyMap());
		// @formatter:on;
	}

	private BootstrapServiceRegistry createBootstrapServiceRegistry(SessionFactoryImplementor sfi) {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating {} ", BootstrapServiceRegistry.class.getName());
		}
		// @formatter:off
		return new BootstrapServiceRegistryBuilder()
				.enableAutoClose()
				.applyClassLoaderService(sfi.getServiceRegistry().requireService(ClassLoaderService.class))
				.build();
		// @formatter:on
	}

	private class ProvidedServicesLocator {

		private static final String CONFIGURATION_FRIENDLY_NONE_VALUE = "none";
		// @formatter:off
		@SuppressWarnings({ "rawtypes" })
		private final List<ProvidedService> providedServices;

		private static final String DEAULT_FILE_RESOURCE_ROOT_DIRECTORY = "files\\";
		
		@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
		public ProvidedServicesLocator(ApplicationContext applicationContext, SessionFactoryImplementor sfi, Environment env) throws Exception {
			final List<ProvidedService> providedServices = new ArrayList<>();
			final ServiceRegistryImplementor serviceRegistry = sfi.getServiceRegistry();
			
			providedServices.add(new ProvidedService<>(JdbcServices.class, sfi.getJdbcServices()));
			providedServices.add(new ProvidedService<>(JdbcEnvironment.class, sfi.getJdbcServices().getJdbcEnvironment()));
			providedServices.add(new ProvidedService<>(RegionFactory.class, serviceRegistry.requireService(RegionFactory.class)));
			
			final String rootDirectory = SpringHelper.getOrDefault(env, Settings.FILE_RESOURCE_ROOT_DIRECTORY, HandledFunction.identity(), DEAULT_FILE_RESOURCE_ROOT_DIRECTORY);
			final String identifierDelimiter = SpringHelper.getOrDefault(env, Settings.FILE_RESOURCE_IDENTIFIER_DELIMITER, HandledFunction.identity(), StringHelper.UNDERSCORE);
			
			providedServices.add(new ProvidedService<>(ConfigurationService.class, new ConfigurationServiceImpl(
					declare(new HashMap())
						.consume(self -> self.putAll(serviceRegistry.requireService(ConfigurationService.class).getSettings()))
						.consume(self -> {
							self.putAll(Map.of(
								AvailableSettings.HBM2DDL_AUTO, CONFIGURATION_FRIENDLY_NONE_VALUE,
								AvailableSettings.HBM2DDL_SCRIPTS_ACTION, CONFIGURATION_FRIENDLY_NONE_VALUE,
								AvailableSettings.HBM2DDL_DATABASE_ACTION, CONFIGURATION_FRIENDLY_NONE_VALUE,
								AvailableSettings.USE_SECOND_LEVEL_CACHE, Boolean.TRUE,
								AvailableSettings.STATEMENT_BATCH_SIZE, 0,
								Settings.FILE_RESOURCE_ROOT_DIRECTORY, rootDirectory,
								Settings.FILE_RESOURCE_IDENTIFIER_DELIMITER, identifierDelimiter,
								Settings.FILE_RESOURCE_IDENTIFIER_LENGTH, SpringHelper.getOrDefault(env, Settings.FILE_RESOURCE_IDENTIFIER_LENGTH, Integer::valueOf, 30)));
						})
						.get())));
			providedServices.add(new ProvidedService<>(ProxyFactoryFactory.class, serviceRegistry.requireService(ProxyFactoryFactory.class)));
			providedServices.add(new ProvidedService<>(CfgXmlAccessService.class, serviceRegistry.requireService(CfgXmlAccessService.class)));
			providedServices.add(new ProvidedService<>(CacheImplementor.class, serviceRegistry.requireService(CacheImplementor.class)));
			providedServices.add(new ProvidedService<>(PersisterFactory.class, serviceRegistry.requireService(PersisterFactory.class)));
			providedServices.add(new ProvidedService<>(PropertyAccessStrategyResolver.class, new PropertyAccessStrategyResolver() {
				@Override
				public PropertyAccessStrategy resolvePropertyAccessStrategy(Class containerClass, String explicitAccessStrategyName,
						EntityMode entityMode) {
					return PropertyAccessStrategyFieldImpl.INSTANCE;
				}
			}));
			providedServices.add(new ProvidedService<>(SessionFactoryServiceRegistryFactory.class, new SessionFactoryServiceRegistryFactory() {
				@Override
				public SessionFactoryServiceRegistry buildServiceRegistry(SessionFactoryImplementor theSfiThatBeingBuilt,
						SessionFactoryOptions sfiOptions) {
					if (logger.isTraceEnabled()) {
						logger.trace("Creating {} ", SessionFactoryServiceRegistry.class.getName());
					}
					
					try {
						return new SessionFactoryServiceRegistryImpl(
								serviceRegistry, // we want to use the service registry from the original SessionFactoryImpl as the parent
								Collections.emptyList(), // we have to initiate all the additional Services before we build our SessionFactory
								ProvidedServicesLocator.this.providedServices, // this SessionFactory must be the original one from Hibernate so that we can collect all the configurations
								theSfiThatBeingBuilt, // this SessionFactory is the one that we are trying to build
								sfiOptions);
					} catch (Exception any) {
						any.printStackTrace();
						return null;
					}
				}
			}));
			
			ManipulationContextImpl manipulationContext = new ManipulationContextImpl(env, identifierDelimiter);
			ImageService imageService = new ImageService(applicationContext, env);
			
			providedServices.add(new ProvidedService<>(SaveStrategyResolver.class, new SaveStrategyResolver(imageService, manipulationContext)));
			providedServices.add(new ProvidedService<>(ManipulationContextImpl.class, manipulationContext));
			providedServices.add(new ProvidedService<>(ImageService.class, imageService));
			providedServices.add(new ProvidedService<>(ZoneContext.class, applicationContext.getBean(ZoneContext.class)));
			providedServices.add(new ProvidedService<>(MutableIdentifierGeneratorFactory.class, serviceRegistry.requireService(MutableIdentifierGeneratorFactory.class)));
			
			this.providedServices = Collections.unmodifiableList(providedServices);
		}
		// @formatter:on
	}

	private class MetadataBuildingOptionsImpl implements MetadataBuildingOptions, JpaOrmXmlPersistenceUnitDefaultAware {

		private final StandardServiceRegistry serviceRegistry;

		private final MappingDefaults mappingDefaults;
		private final ImplicitNamingStrategy implicitNamingStrategy;
		private ReflectionManager reflectionManager;
		private final IdGeneratorStrategyInterpreter idGeneratorStrategyInterpreter;

		public MetadataBuildingOptionsImpl(StandardServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;
			this.mappingDefaults = new MetadataBuilderImpl.MappingDefaultsImpl(this.serviceRegistry);
			this.implicitNamingStrategy = new SpringImplicitNamingStrategy();
			idGeneratorStrategyInterpreter = new IdGeneratorInterpreterImpl();
		}

		public void makeReflectionManager(BootstrapContext boostrapContext) throws Exception {
			this.reflectionManager = declare(new JavaReflectionManager())
					.consume(self -> self.setMetadataProvider(new JPAXMLOverriddenMetadataProvider(boostrapContext)))
					.get();
		}

		@Override
		public StandardServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}

		@Override
		public MappingDefaults getMappingDefaults() {
			return mappingDefaults;
		}

		@Override
		public List<BasicTypeRegistration> getBasicTypeRegistrations() {
			return Collections.emptyList();
		}

		@Override
		public ReflectionManager getReflectionManager() {
			return reflectionManager;
		}

		@Override
		public IndexView getJandexView() {
			return null;
		}

		@Override
		public ScanOptions getScanOptions() {
			return null;
		}

		@Override
		public ScanEnvironment getScanEnvironment() {
			return null;
		}

		@Override
		public Object getScanner() {
			return null;
		}

		@Override
		public ArchiveDescriptorFactory getArchiveDescriptorFactory() {
			return null;
		}

		@Override
		public ClassLoader getTempClassLoader() {
			return null;
		}

		@Override
		public ImplicitNamingStrategy getImplicitNamingStrategy() {
			return implicitNamingStrategy;
		}

		@Override
		public PhysicalNamingStrategy getPhysicalNamingStrategy() {
			return PhysicalNamingStrategyStandardImpl.INSTANCE;
		}

		@Override
		public SharedCacheMode getSharedCacheMode() {
			return SharedCacheMode.ENABLE_SELECTIVE;
		}

		@Override
		public AccessType getImplicitCacheAccessType() {
			return AccessType.TRANSACTIONAL;
		}

		@Override
		public MultiTenancyStrategy getMultiTenancyStrategy() {
			return MultiTenancyStrategy.NONE;
		}

		@Override
		public IdGeneratorStrategyInterpreter getIdGenerationTypeInterpreter() {
			return idGeneratorStrategyInterpreter;
		}

		@Override
		public List<CacheRegionDefinition> getCacheRegionDefinitions() {
			return null;
		}

		@Override
		public boolean ignoreExplicitDiscriminatorsForJoinedInheritance() {
			return false;
		}

		@Override
		public boolean createImplicitDiscriminatorsForJoinedInheritance() {

			return false;
		}

		@Override
		public boolean shouldImplicitlyForceDiscriminatorInSelect() {
			return false;
		}

		@Override
		public boolean useNationalizedCharacterData() {
			return false;
		}

		@Override
		public boolean isSpecjProprietarySyntaxEnabled() {
			return false;
		}

		@Override
		public boolean isNoConstraintByDefault() {
			return false;
		}

		@Override
		public List<MetadataSourceType> getSourceProcessOrdering() {
			return Arrays.asList(MetadataSourceType.CLASS);
		}

		@Override
		public Map<String, SQLFunction> getSqlFunctions() {
			return Collections.emptyMap();
		}

		@Override
		public List<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectList() {
			return Collections.emptyList();
		}

		@Override
		public List<AttributeConverterInfo> getAttributeConverters() {
			return Collections.emptyList();
		}

		@Override
		public boolean isXmlMappingEnabled() {
			return false;
		}

		@Override
		public void apply(JpaOrmXmlPersistenceUnitDefaults jpaOrmXmlPersistenceUnitDefaults) {}

	}

	@Override
	public FileResourceSessionFactory getSessionFactory() {
		return sessionFactory;
	}

}
