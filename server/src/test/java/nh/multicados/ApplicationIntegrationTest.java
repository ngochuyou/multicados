/**
 * 
 */
package nh.multicados;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import multicados.domain.entity.file.UserPhoto;
import multicados.internal.config.WebConfiguration;
import multicados.internal.context.ContextManager;
import multicados.internal.file.domain.Image;
import multicados.internal.file.engine.FileManagement;

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

	private final MockMvc mock;

	@Autowired
	public ApplicationIntegrationTest(MockMvc mock) {
		this.mock = mock;
	}

	@Test
	public void testSaveUserImage() throws IOException {
		Session session = ContextManager.getBean(FileManagement.class).getSessionFactory().openSession();
		Image image = new UserPhoto();

		image.setContent(Files.readAllBytes(
				new File("C:\\Users\\Ngoc Huy\\Pictures\\Saved Pictures\\timofey-urov-WU_y9Iz5x4o-unsplash.jpg")
						.toPath()));
		image.setExtension("jpg");

		session.save(image);

		System.out.println(image.getId());

		session.flush();
		
		System.out.println(image.getId());
	}

	@Test
	private void testGetUserImageBytes() throws Exception {
		MockHttpServletRequestBuilder reqBuilder = MockMvcRequestBuilders.get("/file/user/ngochuy.ou")
				.accept(MediaType.APPLICATION_JSON_VALUE);

		mock.perform(reqBuilder).andExpect(status().isOk()).andDo(result -> {
			System.out.println(result.getResponse().getContentAsString());
		});
	}

}
