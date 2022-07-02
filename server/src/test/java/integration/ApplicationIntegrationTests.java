/**
 * 
 */
package integration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import multicados.domain.entity.file.UserPhoto;
import multicados.internal.config.InternalWebConfiguration;
import multicados.internal.file.engine.FileManagement;
import multicados.internal.file.engine.FileResourcePersister;
import multicados.internal.file.engine.image.ManipulationContext;
import multicados.internal.file.engine.image.Standard;

/**
 * @author Ngoc Huy
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { InternalWebConfiguration.class })
public class ApplicationIntegrationTests {

	private final FileManagement fileManagement;

	@Autowired
	public ApplicationIntegrationTests(FileManagement fileManagement) {
		this.fileManagement = fileManagement;
	}

	@Test
	public void testUserPhotoSave() throws IOException {
		final SessionFactoryImplementor sfi = fileManagement.getSessionFactory();
		final Session session = sfi.openSession();
		final UserPhoto photo = new UserPhoto();

		photo.setContent(Files.readAllBytes(
				new File("C:\\Users\\Ngoc Huy\\Pictures\\Saved Pictures\\imani-bahati-LxVxPA1LOVM-unsplash.jpg")
						.toPath()));
		photo.setExtension("jpg");
		session.save(photo);

		session.flush();

		final FileResourcePersister persister = (FileResourcePersister) sfi.getMetamodel()
				.entityPersister(UserPhoto.class);
		final Standard standard = sfi.getServiceRegistry().requireService(ManipulationContext.class)
				.locateStandard(photo.getId());

		for (final String prefix : standard.getCompressionPrefixes()) {
			Files.delete(new File(persister.resolvePath(String.format("%s_%s", prefix, photo.getId()))).toPath());
		}
	}

}
