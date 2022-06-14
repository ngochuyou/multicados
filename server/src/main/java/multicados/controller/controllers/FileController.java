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

import multicados.domain.entity.entities.User;
import multicados.domain.entity.entities.User_;
import multicados.domain.entity.file.UserPhoto;
import multicados.internal.context.ContextManager;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.file.engine.FileManagement;
import multicados.internal.file.engine.FileResourcePersister;
import multicados.internal.file.engine.FileResourceSessionFactory;
import multicados.internal.helper.Common;
import multicados.internal.helper.Utils.HandledLazySupplier;
import multicados.internal.helper.Utils.LazySupplier;

/**
 * @author Ngoc Huy
 *
 */
@Controller
@RequestMapping("/file")
public class FileController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(FileController.class);

	private final LazySupplier<GenericRepository> genericRepositorySupplier;
	private final SessionFactoryImplementor mainSessionFactory;
	private final LazySupplier<FileResourceSessionFactory> fileResourceSessionFactorySupplier;

	private final HandledLazySupplier<String> userPhotoDirectorySupplier;

	@Autowired
	public FileController(SessionFactory sessionFactory) throws Exception {
		mainSessionFactory = SessionFactoryImplementor.class.cast(sessionFactory);
		genericRepositorySupplier = new LazySupplier<>(() -> ContextManager.getBean(GenericRepository.class));
		fileResourceSessionFactorySupplier = new LazySupplier<>(
				() -> ContextManager.getBean(FileManagement.class).getSessionFactory());
		// @formatter:off
		userPhotoDirectorySupplier = new HandledLazySupplier<>(() ->
				declare(fileResourceSessionFactorySupplier.get())
					.then(SessionFactoryImplementor::getMetamodel)
					.then(metamodel -> metamodel.entityPersister(UserPhoto.class))
					.then(FileResourcePersister.class::cast)
					.then(FileResourcePersister::getDirectoryPath)
					.get());
		// @formatter:on
	}

	@GetMapping("/user/{username}")
	@Transactional(readOnly = true)
	public ResponseEntity<?> getUserPhotoBytesDirectly(@PathVariable("username") String username,
			HttpServletRequest request) throws Exception {
		Optional<Tuple> optionalTuple = genericRepositorySupplier.get().findById(User.class, username,
				(root, query, builder) -> List.of(root.get(User_.photo).alias(User_.PHOTO)),
				mainSessionFactory.getCurrentSession());

		if (optionalTuple.isEmpty()) {
			return notFound(request, List.of(Common.user(username)));
		}
		// @formatter:off
		return declare(optionalTuple.get())
				.then(tuple -> tuple.get(User_.PHOTO, String.class))
					.prepend(userPhotoDirectorySupplier.get())
				.then((directory, filename) -> directory + filename)
					.prepend(request)
				.then(this::doGetBytesDirectly)
				.get();
		// @formatter:on
	}

	private ResponseEntity<?> doGetBytesDirectly(HttpServletRequest request, String path) throws IOException {
		File file = new File(path);

		if (file.exists() && file.isFile()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Directly reading file {}", path);
			}

			return ok(request, Files.readAllBytes(file.toPath()));
		}

		return notFound(request, List.of(Common.file(path)));
	}

}
