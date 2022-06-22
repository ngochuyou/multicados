/**
 * 
 */
package nh.multicados.internal.service.crud.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.domain.entity.Role;
import multicados.domain.entity.entities.Category_;
import multicados.internal.config.WebConfiguration;
import multicados.internal.security.SecurityConfiguration;
import multicados.security.userdetails.UserDetailsServiceImpl.DomainUser;
import nh.multicados.ApplicationIntegrationTest;

/**
 * @author Ngoc Huy
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = ApplicationIntegrationTest.class)
@ContextConfiguration(classes = { WebConfiguration.class, SecurityConfiguration.class })
@TestPropertySource(locations = "classpath:application.properties")
@AutoConfigureMockMvc
public class ReadSecurityTests {

	private static final Logger logger = LoggerFactory.getLogger(ReadSecurityTests.class);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static final DomainUser HEAD_NGOCHUYOU = new DomainUser("ngochuy.ou", "password", true, true,
			LocalDateTime.now(), List.of(new SimpleGrantedAuthority(Role.HEAD.name())));

	private static final DomainUser ANONYMOUS = new DomainUser("anonymous_user", "password", true, true,
			LocalDateTime.now(), List.of(new SimpleGrantedAuthority(Role.ANONYMOUS.name())));

	@SuppressWarnings("unchecked")
	@Test
	public void notEvenHeadCanReadActive() throws Exception {
		// @formatter:off
		final MockHttpServletRequestBuilder reqBuilder = MockMvcRequestBuilders.get("/rest/category")
				.param("attributes", Category_.ACTIVE, Category_.ID, Category_.NAME)
				.with(user(HEAD_NGOCHUYOU))
				.accept(MediaType.APPLICATION_JSON_VALUE);
		// @formatter:on
		mvc.perform(reqBuilder).andExpect(status().isBadRequest()).andDo(result -> {
			logger.info("\n\t" + objectMapper.readValue(result.getResponse().getContentAsString(), Map.class).entrySet()
					.stream()
					.map(entry -> Map.Entry.class.cast(entry).getKey() + ":\t" + Map.Entry.class.cast(entry).getValue())
					.collect(Collectors.joining("\n\t")));
		});
	}

	@Test
	public void anonymousCanReadCategories() throws Exception {
		// @formatter:off
		final MockHttpServletRequestBuilder reqBuilder = MockMvcRequestBuilders.get("/rest/category")
				.with(user(ANONYMOUS))
				.accept(MediaType.APPLICATION_JSON_VALUE);
		// @formatter:on
		mvc.perform(reqBuilder).andExpect(status().isOk()).andDo(result -> {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> categoryList = objectMapper.readValue(result.getResponse().getContentAsString(),
					List.class);

			for (Map<String, Object> map : categoryList) {
				logger.info("\n\t" + map.entrySet().stream().map(entry -> entry.getKey() + ":\t" + entry.getValue())
						.collect(Collectors.joining("\n\t")));
			}
		});
	}

}
