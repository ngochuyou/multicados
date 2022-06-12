/**
 * 
 */
package multicados.internal.security;

import static multicados.internal.helper.Utils.declare;

import java.lang.reflect.InvocationTargetException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import multicados.internal.config.Settings;
import multicados.internal.helper.SpringHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.security.jwt.JWTRequestFilter;
import multicados.internal.security.jwt.JWTSecurityContext;
import multicados.internal.security.jwt.JWTSecurityContextImpl;
import multicados.internal.security.jwt.JWTUsernamePasswordAuthenticationFilter;

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

	private final UserDetailsService userDetailsService;
	private final AuthenticationFailureHandler authenticationFailureHandler;
	private final OnMemoryUserDetailsContext onMemoryUserDetailsContext;
	private JWTSecurityContext jwtSecurityContext;

	public SecurityConfiguration(Environment env) throws Exception {
		this.env = env;
		onMemoryUserDetailsContext = new OnMemoryUserDetailsContextImpl();
		jwtSecurityContext = new JWTSecurityContextImpl(env);
		authenticationFailureHandler = new AuthenticationFailureHandlerImpl();
		userDetailsService = locateUserDetailsService();
	}

	private UserDetailsService locateUserDetailsService() throws Exception {
		// @formatter:off
		return declare(new CustomUserDetailsServiceLocator())
			.then(CustomUserDetailsServiceLocator::locate)
			.then(Optional::ofNullable)
			.then(optional -> optional.orElseGet(() -> super.userDetailsService()))
			.get();
		// @formatter:on
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// @formatter:off
		declare(http)
			.consume(this::csrf)
			.consume(this::cors)
			.consume(this::publicEndpoints)
			.consume(this::securedEndpoints)
			.consume(this::statelessSession)
			.consume(this::jwt);
		// @formatter:on
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService());
	}

	@Override
	protected UserDetailsService userDetailsService() {
		return userDetailsService;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AbstractAuthenticationProcessingFilter jwtUsernamePasswordAuthenticationFilter() throws Exception {
		return new JWTUsernamePasswordAuthenticationFilter(onMemoryUserDetailsContext, jwtSecurityContext,
				authenticationFailureHandler, authenticationManager());
	}

	private boolean isInDevMode() {
		return !env.getProperty(Settings.ACTIVE_PROFILES).equals(Settings.PRODUCTION_PROFILE);
	}

	private void csrf(HttpSecurity http) throws Exception {
		http.csrf().disable();
	}

	private void cors(HttpSecurity http) throws Exception {
		http.cors();
		new DevCorsConfigurer().configure(http);
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

		http.authorizeRequests().antMatchers(HttpMethod.POST, jwtSecurityContext.getTokenEndpoint()).permitAll();
	}

	private void securedEndpoints(HttpSecurity http) throws Exception {
		http.authorizeRequests().anyRequest().authenticated();
	}

	private void statelessSession(HttpSecurity http) throws Exception {
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
	}

	private void jwt(HttpSecurity http) throws Exception {
		declare(http).consume(this::jwtFilters);
	}

	private void jwtFilters(HttpSecurity http) throws Exception {
		// @formatter:off
		http
			.addFilterBefore(
					new JWTRequestFilter(env, userDetailsService(), onMemoryUserDetailsContext, jwtSecurityContext),
					UsernamePasswordAuthenticationFilter.class);
		// @formatter:on
	}

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
				throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
				InvocationTargetException, NoSuchMethodException, SecurityException {
			if (beanFromContext != null) {
				return beanFromContext;
			}

			return UserDetailsService.class
					.cast(TypeHelper.constructFromNonArgs(Class.forName(beanDef.getBeanClassName())));
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
							? String.format("No custom %s found", UserDetailsService.class.getSimpleName())
							: String.format("Located one custom %s of type [%s]",
									UserDetailsService.class.getSimpleName(), bean.getClass().getName())))
				.get();
			// @formatter:on
		}

	}

	private class DevCorsConfigurer {

		private DevCorsConfigurer() {}

		public void configure(HttpSecurity http) throws Exception {
			if (!isInDevMode()) {
				return;
			}
			// @formatter:off
			CorsConfiguration configuration = declare(new CorsConfiguration())
					.consume(self -> self.setAllowCredentials(true)).get();
			DatagramSocket socket = declare(new DatagramSocket()).consume(self -> self.connect(InetAddress.getByName("8.8.8.8"), 10002)).get(); 
			String clientURLTemplate = String.format("http://%s:%s", socket.getLocalAddress().getHostAddress(), "%s");
			
			socket.close();
			
			declare(Stream.of(SpringHelper.getArrayOrDefault(env, Settings.SECURITY_DEV_CLIENT_PORTS, Function.identity(), StringHelper.EMPTY_STRING_ARRAY))
						.map(clientPort -> String.format(clientURLTemplate, clientPort))
						.toArray(String[]::new))
				.then(Arrays::asList)
				.consume(configuration::setAllowedOrigins);
			declare(Arrays.asList(
					HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
					HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()))
				.consume(configuration::setAllowedMethods);
			declare(Arrays.asList(
					HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE,
					HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
				.consume(configuration::setAllowedHeaders);
			// @formatter:on
			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

			source.registerCorsConfiguration("/**", configuration);
			http.addFilterAt(new CorsFilter(source), CorsFilter.class);
		}

	}

}
