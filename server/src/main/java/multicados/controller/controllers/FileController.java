/**
 *
 */
package multicados.controller.controllers;

import static multicados.internal.helper.Utils.declare;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import javax.persistence.Tuple;
import javax.servlet.http.HttpServletRequest;

import org.hibernate.SessionFactory;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import multicados.domain.entity.entities.User;
import multicados.domain.entity.entities.User_;
import multicados.domain.entity.file.UserPhoto;
import multicados.internal.config.Settings;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.file.engine.FileManagement;
import multicados.internal.file.engine.FileResourcePersister;
import multicados.internal.file.engine.FileResourceSessionFactory;
import multicados.internal.file.engine.image.ManipulationContext;
import multicados.internal.helper.Common;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
@Controller
@RequestMapping("/file")
public class FileController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(FileController.class);

	private final GenericRepository genericRepository;
	private final SessionFactoryImplementor mainSessionFactory;

	private final String userPhotoDirectory;
	private final String publicDirectory;
	private final ManipulationContext manipulationContext;

	@Autowired
	public FileController(
	// @formatter:off
			SessionFactory sessionFactory,
			GenericRepository genericRepository,
			FileManagement fileManagement) throws Exception {
		// @formatter:on
		mainSessionFactory = SessionFactoryImplementor.class.cast(sessionFactory);

		final FileResourceSessionFactory fileResourceSessionFactory = fileManagement.getSessionFactory();

		this.genericRepository = genericRepository;
		// @formatter:off
		userPhotoDirectory = declare(fileResourceSessionFactory)
				.then(SessionFactoryImplementor::getMetamodel)
				.then(metamodel -> metamodel.entityPersister(UserPhoto.class))
				.then(FileResourcePersister.class::cast)
				.then(FileResourcePersister::getDirectoryPath)
				.get();
		manipulationContext = fileResourceSessionFactory.getServiceRegistry().requireService(ManipulationContext.class);
		publicDirectory = (String) fileResourceSessionFactory.getServiceRegistry().requireService(ConfigurationService.class).getSettings().get(Settings.FILE_RESOURCE_PUBLIC_DIRECTORY);
		// @formatter:on
	}

	@GetMapping("/public/{filename}")
	@Transactional(readOnly = true)
	public byte[] getPublicResource(@PathVariable("filename") String filename, HttpServletRequest request)
			throws IOException {
		return Files.readAllBytes(new File(publicDirectory + filename).toPath());
	}

	@GetMapping("/public/user/{username}")
	@Transactional(readOnly = true)
	public ResponseEntity<?> getUserPhotoBytesDirectly(
	// @formatter:off
			@PathVariable("username") String username,
			@RequestParam(name = "size", required = false, defaultValue = StringHelper.EMPTY_STRING) String size,
			HttpServletRequest request) throws Exception {
		// @formatter:on
		final Optional<Tuple> optionalTuple = genericRepository.findById(User.class, username,
				(root, query, builder) -> List.of(root.get(User_.photo).alias(User_.PHOTO)),
				mainSessionFactory.getCurrentSession());

		if (optionalTuple.isEmpty()) {
			return sendNotFound(List.of(Common.user(username)), request);
		}
		// @formatter:off
		return declare(optionalTuple.get())
				.then(tuple -> tuple.get(User_.PHOTO, String.class))
					.prepend(userPhotoDirectory)
				.then((directory, filename) -> directory + (size.isEmpty() ? filename : manipulationContext.resolveCompressionName(filename, size)))
					.prepend(request)
				.then(this::doGetBytesDirectly)
				.get();
		// @formatter:on
	}

	private ResponseEntity<?> doGetBytesDirectly(HttpServletRequest request, String path) throws IOException {
		final File file = new File(path);

		if (file.exists() && file.isFile()) {
			logger.debug("Directly reading file {}", path);

			return sendOk(Files.readAllBytes(file.toPath()), request);
		}

		return sendNotFound(List.of(Common.file(path)), request);
	}

}
