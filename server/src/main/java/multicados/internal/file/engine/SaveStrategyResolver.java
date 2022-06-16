/**
 * 
 */
package multicados.internal.file.engine;

import static multicados.internal.helper.Utils.declare;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.hibernate.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.file.domain.FileResource;
import multicados.internal.file.domain.Image;
import multicados.internal.file.engine.image.ImageService;
import multicados.internal.file.engine.image.ManipulationContext;
import multicados.internal.file.engine.image.Standard;
import multicados.internal.helper.Utils.BiDeclaration;

/**
 * @author Ngoc Huy
 *
 */
public class SaveStrategyResolver implements Service {

	private static final long serialVersionUID = 1L;

	private final Map<Class<? extends FileResource>, SaveStrategy> saveStrategies;

	public SaveStrategyResolver(ImageService imageService, ManipulationContext manipulationContext) {
		saveStrategies = Map.of(FileResource.class, new DefaultSaveStrategy(), Image.class,
				new ImageSaveStrategy(imageService, manipulationContext));
	}

	public <T extends FileResource> SaveStrategy getSaveStrategy(Class<T> resourceType) {
		return saveStrategies.get(resourceType);
	}

	private class DefaultSaveStrategy implements SaveStrategy {

		private static final Logger logger = LoggerFactory.getLogger(DefaultSaveStrategy.class);

		private void logInsert(Path path, byte[] content) {
			if (logger.isDebugEnabled()) {
				logger.debug("Writing file [{}] with content length {}", path, content.length);
			}
		}

		@Override
		public <T extends FileResource> void save(FileResourcePersister persister, Serializable id, T file,
				FileResourceSession session) throws Exception {
			// @formatter:off
			declare(id.toString())
				.then(persister::resolvePath)
				.then(File::new)
				.then(File::getPath)
				.then(Paths::get)
					.second(file.getContent())
				.consume(this::logInsert)
				.consume(Files::write);
			// @formatter:on
		}

	}

	private class ImageSaveStrategy extends DefaultSaveStrategy implements SaveStrategy {

		private static final Logger logger = LoggerFactory.getLogger(ImageSaveStrategy.class);

		private final ImageService imageService;
		private final ManipulationContext manipulationContext;

		public ImageSaveStrategy(ImageService imageService, ManipulationContext manipulationContext) {
			this.imageService = imageService;
			this.manipulationContext = manipulationContext;
		}

		@Override
		public <T extends FileResource> void save(FileResourcePersister persister, Serializable id, T object,
				FileResourceSession session) throws Exception {
			Image image = (Image) object;
			BiDeclaration<Standard, byte[][]> manipulation = imageService.adjustAndPropagate(image);
			Standard standard = manipulation.getFirst();
			byte[][] contentArray = manipulation.getSecond();

			doSave(session, persister, image, id.toString(), contentArray[0], standard);

			int batchSize = standard.getBatchSize();
			String[] compressionPrefixes = standard.getCompressionPrefixes();

			for (int i = 1; i < batchSize; i++) {
				// @formatter:off
				doSave(
						session,
						persister,
						image,
						manipulationContext.resolveCompressionName(id.toString(), compressionPrefixes[i - 1]),
						contentArray[i],
						standard);
				// @formatter:on
			}
		}

		private void doSave(FileResourceSession session, FileResourcePersister persister, Image originalImage,
				String id, byte[] content, Standard standard) throws Exception {
			// @formatter:off
			super.save(
					persister,
					id,
					declare(originalImage)
						.then(persister::getPropertyValues)
							.prepend(declare(id, session)
										.then(persister::instantiate)
										.then(Image.class::cast))
						.consume(persister::setPropertyValues)
						.useFirst()
						.consume(actualImage -> actualImage.setContent(content))
						.consume(image -> {
							if (logger.isDebugEnabled()) {
								logger.debug("Saving an {} with {}, buffer length: {}", Image.class.getSimpleName(), standard,
										content.length);
							}
						})
						.get(),
					session);
			// @formatter:on
		}

	}

}
