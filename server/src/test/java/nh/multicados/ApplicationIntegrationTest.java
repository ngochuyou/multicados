/**
 * 
 */
package nh.multicados;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import multicados.domain.entity.file.UserPhoto;
import multicados.internal.config.WebConfiguration;
import multicados.internal.context.ContextManager;
import multicados.internal.file.engine.FileManagement;
import multicados.internal.file.engine.FileResourceSessionFactory;
import multicados.internal.helper.Utils;

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

	@Test
	public void test() throws Exception {
		final FileResourceSessionFactory sfi = ContextManager.getBean(FileManagement.class).getSessionFactory();
		final Session session = Utils.declare(sfi.openSession()).consume(ss -> ss.beginTransaction()).get();
		final UserPhoto photo = new UserPhoto();

		photo.setContent(new byte[] { 0xa, 0x4 });
		photo.setExtension(".jpg");

		session.save(photo);
		session.flush();
	}

}
