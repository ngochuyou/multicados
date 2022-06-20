/**
 * 
 */
package nh.multicados.internal.file;

import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import multicados.internal.config.Settings;
import multicados.internal.file.engine.FileManagement;
import multicados.internal.file.engine.FileManagementImpl;
import multicados.internal.locale.ZoneContext;
import multicados.internal.locale.ZoneContextImpl;

/**
 * @author Ngoc Huy
 *
 */
@Configuration
public class FileResourceConfiguration {

	@Bean
	public ZoneContext zoneContext(Environment env) {
		return new ZoneContextImpl(env);
	}

	@Bean
	public FileManagement fileManagement(ApplicationContext applicationContext, Environment env,
			SessionFactory sessionFactory) throws Exception {
		return new FileManagementImpl(applicationContext, env, sessionFactory.unwrap(SessionFactoryImplementor.class));
	}

	@Bean
	public FactoryBean<SessionFactory> sessionFactory(DataSource dataSource, Environment env) {
		LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();

		sessionFactory.setDataSource(dataSource);
		sessionFactory.setPackagesToScan(new String[] { env.getProperty(Settings.SCANNED_ENTITY_PACKAGES) });
		// snake_case for columns
		sessionFactory.setPhysicalNamingStrategy(new CamelCaseToUnderscoresNamingStrategy());

		Properties properties = new Properties();

		properties.put(AvailableSettings.DIALECT, "org.hibernate.dialect.MySQL8Dialect");
		properties.put(AvailableSettings.SHOW_SQL, true);
		properties.put(AvailableSettings.FORMAT_SQL, true);
		properties.put(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
		properties.put(AvailableSettings.HBM2DDL_AUTO, "update");
		properties.put(AvailableSettings.STATEMENT_BATCH_SIZE, 50);
		properties.put(AvailableSettings.ORDER_INSERTS, true);
		properties.put(AvailableSettings.ORDER_UPDATES, true);

		sessionFactory.setHibernateProperties(properties);

		return sessionFactory;
	}

	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();

		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl(
				"jdbc:mysql://localhost:3306/multicados?serverTimezone=UTC&useUnicode=yes&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&jdbcCompliantTruncation=false&serverTimezone=GMT%2B7&useLegacyDatetimeCode=false");
		dataSource.setUsername("root");
		dataSource.setPassword("root");

		return dataSource;
	}

}
