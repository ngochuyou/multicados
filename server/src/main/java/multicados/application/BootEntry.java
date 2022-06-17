/**
 * 
 */
package multicados.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

import multicados.internal.config.DomainLogicContextConfiguration;
import multicados.internal.config.Settings;

/**
 * Application entry point
 * 
 * @author Ngoc Huy
 *
 */
// @formatter:off
@SpringBootApplication(
		exclude = HibernateJpaAutoConfiguration.class,
		scanBasePackages = Settings.BASE_PACKAGE)
@EnableCaching(proxyTargetClass = true)
// @formatter:on
@Import(DomainLogicContextConfiguration.class)
public class BootEntry {

	public static void main(String[] args) {
		SpringApplication.run(BootEntry.class, args);
	}

}
