/**
 * 
 */
package nh.multicados;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import multicados.internal.config.WebConfiguration;

/**
 * @author Ngoc Huy
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = ApplicationIntegrationTest.class)
//@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(classes = { WebConfiguration.class })
@AutoConfigureMockMvc
public class ApplicationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void test() throws Exception {
		// @formatter:off
		MvcResult result = mockMvc
				.perform(MockMvcRequestBuilders
						.get("/rest/district?attributes=id,name,active&province.attributes=id,name&name.like=abc"))
				.andReturn();
		// @formatter:on
		assertTrue(result.getResponse().getStatus() == HttpServletResponse.SC_OK);
		System.out.println(result.getResponse().getContentAsString());
	}

}
