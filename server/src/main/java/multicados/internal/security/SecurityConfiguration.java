/**
 * 
 */
package multicados.internal.security;

import static multicados.internal.helper.Utils.declare;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextManager;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
@ComponentScan(basePackages = { Settings.BASE_PACKAGE })
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);

	private final Environment env;

	public SecurityConfiguration(Environment env) {
		this.env = env;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// @formatter:off
		declare(http)
			.consume(this::csrf)
			.consume(this::cors)
			.consume(this::publicEndpoints)
			.consume(this::tokenEndpoint)
			.consume(this::securedEndpoints)
			.consume(this::statelessSession)
			.consume(this::authorizationFilters)
			.consume(this::authenticationFilters)
			.consume(this::exceptionHandling);
		// @formatter:on
	}

	@Override
	protected UserDetailsService userDetailsService() {
		try {
			// @formatter:off
			return Utils.declare(new CustomUserDetailsServiceLocator())
				.then(CustomUserDetailsServiceLocator::locate)
				.then(Optional::ofNullable)
				.then(optional -> optional.orElseGet(() -> super.userDetailsService()))
				.get();
			// @formatter:on
		} catch (Exception any) {
			any.printStackTrace();
			SpringApplication.exit(ContextManager.getExitAcess().getContext());
			return null;
		}
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private boolean isInDevMode() {
		return !env.getProperty("spring.profiles.active").equals("PROD");
	}

	private void csrf(HttpSecurity http) throws Exception {
		http.csrf().disable();
	}

	private void cors(HttpSecurity http) throws Exception {
		http.cors();
	}

	private static final String ENDPOINT_PATTERN_PARTS_DELIMITER = "\\\\";

	private void publicEndpoints(HttpSecurity http) throws Exception {
		String[] patterns = env.getProperty(Settings.SECURITY_PUBLIC_END_POINTS, String[].class,
				StringHelper.EMPTY_STRING_ARRAY);

		for (String pattern : patterns) {
			String[] parts = pattern.split(ENDPOINT_PATTERN_PARTS_DELIMITER);

			if (parts.length == 0) {
				continue;
			}

			if (parts.length == 1) {
				http.authorizeRequests().antMatchers(parts[0]).permitAll();
				continue;
			}

			String[] methods = parts[1].split(StringHelper.WHITESPACE_CHAR_CLASS);

			for (String method : methods) {
				HttpMethod httpMethod = HttpMethod.resolve(method);

				if (httpMethod == null) {
					continue;
				}

				http.authorizeRequests().antMatchers(httpMethod, parts[0]).permitAll();
			}
		}
	}

	private void tokenEndpoint(HttpSecurity http) throws Exception {
		// @formatter:off
		http.authorizeRequests()
			.antMatchers(HttpMethod.POST, env.getProperty(Settings.SECURITY_TOKEN_END_POINT))
			.permitAll();
		// @formatter:on
	}

	private void securedEndpoints(HttpSecurity http) throws Exception {
		http.authorizeRequests().anyRequest().authenticated();
	}

	private void statelessSession(HttpSecurity http) throws Exception {
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
	}

	private void authorizationFilters(HttpSecurity http) {}

	private void authenticationFilters(HttpSecurity http) {}

	private void exceptionHandling(HttpSecurity http) {}

	private class CustomUserDetailsServiceLocator {

		private CustomUserDetailsServiceLocator() {}

		private ClassPathScanningCandidateComponentProvider getScanner() throws Exception {
			return declare(new ClassPathScanningCandidateComponentProvider(false, env))
					.consume(scanner -> scanner.addIncludeFilter(new AssignableTypeFilter(UserDetailsService.class)))
					.get();
		}

		private Optional<BeanDefinition> scan(ClassPathScanningCandidateComponentProvider scanner) {
			return scanner.findCandidateComponents(Settings.BASE_PACKAGE).stream().findFirst();
		}

		@SuppressWarnings("unchecked")
		private UserDetailsService tryToLocateFromExistingContext(BeanDefinition beanDef) {
			ApplicationContext applicationContext = SecurityConfiguration.super.getApplicationContext();

			try {
				Class<UserDetailsService> clazz = (Class<UserDetailsService>) Class.forName(beanDef.getBeanClassName());

				if (clazz.isAnnotationPresent(Component.class) || clazz.isAnnotationPresent(Service.class)) {
					return applicationContext.getBean(clazz);
				}

				return applicationContext.getBean(
						Optional.ofNullable(beanDef.getFactoryBeanName()).orElse(beanDef.getBeanClassName()),
						UserDetailsService.class);
			} catch (Exception any) {
				logger.trace(String.format("Unable to locate %s from application context, error: %s",
						UserDetailsService.class.getSimpleName(), any.getMessage()));
				return null;
			}
		}

		private UserDetailsService tryToConstruct(BeanDefinition beanDef, UserDetailsService beanFromContext)
				throws ClassNotFoundException {
			if (beanFromContext != null) {
				return beanFromContext;
			}

			ContextManager.registerBean(beanDef.getBeanClassName(), beanDef);

			return (UserDetailsService) ContextManager.getBean(Class.forName(beanDef.getBeanClassName()));
		}

		private UserDetailsService doLocate(Optional<BeanDefinition> optionalBeanDef) throws Exception {
			if (optionalBeanDef.isEmpty()) {
				return null;
			}
			// @formatter:off
			return declare(optionalBeanDef.get())
						.second(this::tryToLocateFromExistingContext)
					.then(this::tryToConstruct)
					.get();
			// @formatter:on
		}

		public UserDetailsService locate() throws Exception {
			// @formatter:off
			return declare(getScanner())
				.then(this::scan)
				.then(this::doLocate)
				.consume(bean -> logger.trace(bean == null
							? String.format("No custom %s", UserDetailsService.class.getSimpleName())
							: String.format("Located one custom %s of type [%s]",
									UserDetailsService.class.getSimpleName(), bean.getClass().getName())))
				.get();
			// @formatter:on
		}

	}

}
