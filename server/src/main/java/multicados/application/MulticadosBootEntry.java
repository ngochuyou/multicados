/**
 *
 */
package multicados.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

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
// @formatter:on
public class MulticadosBootEntry {

	public static void main(String[] args) {
		SpringApplication.run(MulticadosBootEntry.class, args);
	}

}
