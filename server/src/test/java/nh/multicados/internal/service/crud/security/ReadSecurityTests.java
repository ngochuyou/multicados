/**
 * 
 */
package nh.multicados.internal.service.crud.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.domain.entity.entities.Category_;
import multicados.internal.config.WebConfiguration;
import multicados.internal.security.SecurityConfiguration;
import nh.multicados.AbstractTest;

/**
 * @author Ngoc Huy
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = { WebConfiguration.class, SecurityConfiguration.class })
@TestPropertySource(locations = "classpath:application.properties")
@AutoConfigureMockMvc
public class ReadSecurityTests extends AbstractTest {

	private final MockMvc mvc;

	@Autowired
	public ReadSecurityTests(ObjectMapper objectMapper, MockMvc mvc) {
		super(objectMapper);
		this.mvc = mvc;
	}

	@Test
	public void notEvenHeadCanReadActive() throws Exception {
		// @formatter:off
		final MockHttpServletRequestBuilder reqBuilder = MockMvcRequestBuilders.get("/rest/category")
				.param("attributes", Category_.ACTIVE, Category_.ID, Category_.NAME)
				.with(user(AbstractTest.HEAD_NGOCHUYOU))
				.accept(MediaType.APPLICATION_JSON_VALUE);
		// @formatter:on
		mvc.perform(reqBuilder).andExpect(status().isBadRequest())
				.andDo(result -> logJson(result.getResponse().getContentAsString(), Map.class));
	}

	@Test
	public void anonymousCanReadCategories() throws Exception {
		// @formatter:off
		final MockHttpServletRequestBuilder reqBuilder = MockMvcRequestBuilders.get("/rest/category")
				.with(user(AbstractTest.ANONYMOUS))
				.accept(MediaType.APPLICATION_JSON_VALUE);
		// @formatter:on
		mvc.perform(reqBuilder).andExpect(status().isOk())
				.andDo(result -> logJson(result.getResponse().getContentAsString(), List.class));
	}

}