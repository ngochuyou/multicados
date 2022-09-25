/**
 *
 */
package multicados.internal.security;

import static multicados.internal.helper.Utils.declare;

import java.util.Arrays;
import java.util.stream.Stream;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.internal.config.Settings;
import multicados.internal.helper.DNSUtils;
import multicados.internal.helper.SpringHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils.HandledFunction;
import multicados.internal.locale.ZoneContext;
import multicados.internal.security.jwt.HeaderBasedJwtAdvisor;
import multicados.internal.security.jwt.JwtAdvisor;
import multicados.internal.security.jwt.JwtLogoutFilter;
import multicados.internal.security.jwt.JwtRequestFilter;
import multicados.internal.security.jwt.JwtSecurityContext;
import multicados.internal.security.jwt.JwtSecurityContextImpl;
import multicados.internal.security.jwt.JwtUsernamePasswordAuthenticationFilter;

/**
 * @author Ngoc Huy
 *
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class InternalSecurityConfiguration {

	private final Environment env;

	private final UserDetailsService userDetailsService;
	private final AuthenticationFailureHandler authenticationFailureHandler;
	private final OnMemoryUserDetailsContext onMemoryUserDetailsContext;
	private final JwtSecurityContext jwtSecurityContext;
	private final ObjectMapper objectMapper;

	private final AccessDeniedHandler accessDeniedHandler;
	private final AuthenticationEntryPoint accessDeniedAuthenticationEntryPoint;
	private final FilterChainExceptionHandlingFilter chainExceptionHandlingFilter;

	private final JwtAdvisor jwtAdvisor;
	private final JwtRequestFilter jwtRequestFilter;

	public InternalSecurityConfiguration(Environment env, ZoneContext zoneContext, ObjectMapper objectMapper,
			UserDetailsService userDetailsService) throws Exception {
		this.env = env;
		this.objectMapper = objectMapper;
		onMemoryUserDetailsContext = new OnMemoryUserDetailsContextImpl();
		jwtSecurityContext = new JwtSecurityContextImpl(env, zoneContext);
		authenticationFailureHandler = new AuthenticationFailureHandlerImpl(objectMapper);
		this.userDetailsService = userDetailsService;

		jwtAdvisor = new HeaderBasedJwtAdvisor(jwtSecurityContext);
		jwtRequestFilter = new JwtRequestFilter(env, jwtSecurityContext, jwtAdvisor, userDetailsService,
				onMemoryUserDetailsContext, objectMapper);

		accessDeniedHandler = new AccessDeniedHandlerImpl(objectMapper);
		accessDeniedAuthenticationEntryPoint = new AccessDeniedAuthenticationEntryPoint(accessDeniedHandler);
		chainExceptionHandlingFilter = new FilterChainExceptionHandlingFilter(accessDeniedHandler);
	}

	@Bean
	public OnMemoryUserDetailsContext onMemoryUserDetailsContext() {
		return onMemoryUserDetailsContext;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, DNSUtils dnsUtils) throws Exception {
		// @formatter:off
		declare(http)
			.consume(this::cacheRequests)
			.consume(this::csrf)
				.second(dnsUtils)
			.consume(this::cors)
			.consume(this::publicEndpoints)
			.consume(this::securedEndpoints)
			.consume(this::accessDenied)
			.consume(this::statelessSession)
			.consume(this::noLogout)
			.consume(this::jwt);
		// @formatter:on
		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		final AuthenticationManagerBuilder authenticationManagerBuilder = http
				.getSharedObject(AuthenticationManagerBuilder.class);

		authenticationManagerBuilder.userDetailsService(userDetailsService);

		final AuthenticationManager authenticationManager = authenticationManagerBuilder.build();

		http.authenticationManager(authenticationManager);

		return authenticationManager;
	}

	@Bean
	public JwtSecurityContext jwtSecurityContext() {
		return jwtSecurityContext;
	}
	
	@Bean
	public AbstractAuthenticationProcessingFilter jwtUsernamePasswordAuthenticationFilter(
			ApplicationContext applicationContext, HttpSecurity http) throws Exception {
		return new JwtUsernamePasswordAuthenticationFilter(onMemoryUserDetailsContext, jwtSecurityContext,
				authenticationFailureHandler, applicationContext.getBean(AuthenticationManager.class), objectMapper);
	}

	@Bean
	public AbstractAuthenticationProcessingFilter jwtLogoutFilter(ApplicationContext applicationContext,
			HttpSecurity http) throws Exception {
		return new JwtLogoutFilter(jwtSecurityContext, applicationContext.getBean(AuthenticationManager.class),
				objectMapper, jwtAdvisor);
	}

	private boolean isInDevMode() {
		return !env.getProperty(Settings.ACTIVE_PROFILES).equals(Settings.DEFAULT_PRODUCTION_PROFILE);
	}

	private void accessDenied(HttpSecurity http) throws Exception {
		// @formatter:off
		http
			.exceptionHandling()
				.accessDeniedHandler(accessDeniedHandler)
				.authenticationEntryPoint(accessDeniedAuthenticationEntryPoint)
			.and()
			.addFilterBefore(chainExceptionHandlingFilter, FilterSecurityInterceptor.class);
		// @formatter:on
	}

	private void cacheRequests(HttpSecurity http) throws Exception {
		http.requestCache();
	}

	private void csrf(HttpSecurity http) throws Exception {
		http.csrf().disable();
	}

	private void cors(HttpSecurity http, DNSUtils dnsUtils) throws Exception {
		new CorsConfigurer().configure(http, dnsUtils);
	}

	private static final String ENDPOINT_PATTERN_PARTS_DELIMITER = "\\\\";

	private void publicEndpoints(HttpSecurity http) throws Exception {
		final String[] patterns = env.getProperty(Settings.SECURITY_PUBLIC_END_POINTS, String[].class,
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
		// @formatter:off
		http.authorizeRequests()
			.antMatchers(HttpMethod.POST, jwtSecurityContext.getTokenEndpoint()).permitAll()
			.antMatchers(HttpMethod.POST, jwtSecurityContext.getLogoutEndpoint()).permitAll();
		// @formatter:on
	}

	private void securedEndpoints(HttpSecurity http) throws Exception {
		http.authorizeRequests().anyRequest().authenticated();
	}

	private void statelessSession(HttpSecurity http) throws Exception {
		// @formatter:off
		http
			.sessionManagement()
				.disable()
			.rememberMe()
				.disable();
		// @formatter:on
	}

	private void noLogout(HttpSecurity http) throws Exception {
		http.logout().disable();
	}

	private void jwt(HttpSecurity http) throws Exception {
		declare(http).consume(this::jwtFilters);
	}

	private void jwtFilters(HttpSecurity http) throws Exception {
		// @formatter:off
		http
			.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
		// @formatter:on
	}

	private class CorsConfigurer {

		private CorsConfigurer() {}

		public void configure(HttpSecurity http, DNSUtils dnsUtils) throws Exception {
			if (!isInDevMode()) {
				http.cors();
				return;
			}
			// @formatter:off
			final CorsConfiguration configuration = declare(new CorsConfiguration())
					.consume(self -> self.setAllowCredentials(true)).get();
			final String clientURLTemplate = String.format("http://%s:%s", dnsUtils.getHostAddress(), "%s");

			declare(Stream.of(
						SpringHelper.getArrayOrDefault(env, Settings.SECURITY_DEV_CLIENT_PORTS, HandledFunction.identity(), StringHelper.EMPTY_STRING_ARRAY))
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
