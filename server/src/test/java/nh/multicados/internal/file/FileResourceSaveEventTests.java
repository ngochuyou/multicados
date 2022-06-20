/**
 * 
 */
package nh.multicados.internal.file;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.hibernate.Session;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import multicados.domain.entity.file.UserPhoto;
import multicados.internal.config.Settings;
import multicados.internal.file.domain.Image;
import multicados.internal.file.engine.FileManagement;
import multicados.internal.file.engine.FileResourcePersister;
import multicados.internal.file.engine.image.ManipulationContext;
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.BiDeclaration;

/**
 * @author Ngoc Huy
 *
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { FileResourceConfiguration.class })
@TestPropertySource(locations = "classpath:application.properties")
public class FileResourceSaveEventTests {

	private final FileManagement fileManagement;

	@Autowired
	public FileResourceSaveEventTests(FileManagement fileManagement) {
		this.fileManagement = fileManagement;
	}

	@Test
	private BiDeclaration<Session, Image[]> canGetImageNameWithStandardAfterInvokingSaveEvent() throws IOException {
		Session session = fileManagement.getSessionFactory().openSession();
		Image square = new UserPhoto();

		square.setContent(Files.readAllBytes(
				new File("C:\\Users\\Ngoc Huy\\Pictures\\Saved Pictures\\imani-bahati-LxVxPA1LOVM-unsplash.jpg")
						.toPath()));
		square.setExtension("jpg");

		session.save(square);

		assertTrue(String.format("Produced %s while expecting qan", square.getId()), square.getId().startsWith("qan"));

		Image landscape = new UserPhoto();

		landscape.setContent(Files.readAllBytes(
				new File("C:\\Users\\Ngoc Huy\\Pictures\\Saved Pictures\\atikh-bana-_KaMTEmJnxY-unsplash.jpg")
						.toPath()));
		landscape.setExtension("jpg");

		session.save(landscape);

		assertTrue(String.format("Produced %s while expecting jue", landscape.getId()),
				landscape.getId().startsWith("jue"));

		Image portrait = new UserPhoto();

		portrait.setContent(Files.readAllBytes(
				new File("C:\\Users\\Ngoc Huy\\Pictures\\Saved Pictures\\brooke-cagle-HRZUzoX1e6w-unsplash.jpg")
						.toPath()));
		portrait.setExtension("jpg");

		session.save(portrait);

		assertTrue(String.format("Produced %s while expecting om$", portrait.getId()),
				portrait.getId().startsWith("om$"));

		return Utils.declare(session, new Image[] { square, landscape, portrait });
	}

	@Test
	public void canBeSaved() throws IOException {
		BiDeclaration<Session, Image[]> declaration = canGetImageNameWithStandardAfterInvokingSaveEvent();
		Session session = declaration.getFirst();
		FileResourcePersister persister = (FileResourcePersister) session.getSessionFactory()
				.unwrap(SessionFactoryImplementor.class).getMetamodel().entityPersister(UserPhoto.class);
		Image[] images = declaration.getSecond();
		String delimiter = session.getSessionFactory().unwrap(SessionFactoryImplementor.class).getServiceRegistry()
				.requireService(ConfigurationService.class).getSettings()
				.get(Settings.FILE_RESOURCE_IDENTIFIER_DELIMITER).toString();
		ManipulationContext manipulationContext = session.getSessionFactory().unwrap(SessionFactoryImplementor.class)
				.getServiceRegistry().requireService(ManipulationContext.class);

		session.flush();

		for (Image image : images) {
			for (String prefix : manipulationContext.locateStandard(image.getId()).getCompressionPrefixes()) {
				Files.delete(new File(persister.resolvePath(String.format("%s%s%s", prefix, delimiter, image.getId())))
						.toPath());
			}
		}
	}

}
