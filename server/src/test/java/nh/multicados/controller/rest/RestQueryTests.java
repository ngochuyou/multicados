/**
 * 
 */
package nh.multicados.controller.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import multicados.domain.entity.entities.District_;
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
public class RestQueryTests extends AbstractTest {

	private final MockMvc mvc;

	@Autowired
	public RestQueryTests(ObjectMapper objectMapper, MockMvc mvc) {
		super(objectMapper);
		this.mvc = mvc;
	}

	@Test
	public void canRequestSpecificAttributes() throws Exception {
		// @formatter:off
		MockHttpServletRequestBuilder reqBuilder = MockMvcRequestBuilders.get("/rest/category")
				.param("attributes", Category_.ID, Category_.NAME, Category_.DESCRIPTION)
				.with(user(AbstractTest.HEAD_NGOCHUYOU))
				.accept(MediaType.APPLICATION_JSON_VALUE);
		
		mvc.perform(reqBuilder).andExpect(status().isOk())
				.andDo(result -> logJson(result.getResponse().getContentAsString(), List.class));
		// @formatter:on
	}

	@Test
	public void canRequestSpecificAttributesAndCanBePaged() throws Exception {
		// @formatter:off
		MockHttpServletRequestBuilder reqBuilder = MockMvcRequestBuilders.get("/rest/location/district")
				.param("attributes", District_.ID, District_.NAME)
				.param("page.size", "2")
				.param("page.num", "10").with(user(AbstractTest.HEAD_NGOCHUYOU))
				.accept(MediaType.APPLICATION_JSON_VALUE);
		// @formatter:on
		mvc.perform(reqBuilder).andExpect(status().isOk())
				.andDo(result -> logJson(result.getResponse().getContentAsString(), List.class));
	}

}
