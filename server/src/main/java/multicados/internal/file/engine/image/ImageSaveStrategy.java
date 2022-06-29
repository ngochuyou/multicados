/**
 *
 */
package multicados.internal.file.engine.image;

import static multicados.internal.helper.Utils.declare;

import multicados.internal.file.domain.FileResource;
import multicados.internal.file.domain.Image;
import multicados.internal.file.engine.DefaultSaveStrategy;
import multicados.internal.file.engine.FileResourcePersister;
import multicados.internal.file.engine.FileResourceSession;
import multicados.internal.file.engine.SaveStrategy;
import multicados.internal.helper.Utils.BiDeclaration;

public class ImageSaveStrategy extends DefaultSaveStrategy implements SaveStrategy {

	private final ImageService imageService;
	private final ManipulationContext manipulationContext;

	public ImageSaveStrategy(ImageService imageService, ManipulationContext manipulationContext) {
		this.imageService = imageService;
		this.manipulationContext = manipulationContext;
	}

	@Override
	public <T extends FileResource> String save(FileResourcePersister persister, String id, T object,
			FileResourceSession session) throws Exception {
		Image image = (Image) object;
		BiDeclaration<Standard, byte[][]> manipulation = imageService.adjustAndPropagate(image);
		Standard standard = manipulation.getFirst();
		byte[][] contentArray = manipulation.getSecond();
		String[] compressionPrefixes = standard.getCompressionPrefixes();

		doSave(session, persister, image, manipulationContext.resolveCompressionName(id, compressionPrefixes[0]),
				contentArray[0]);

		int batchSize = standard.getBatchSize();

		for (int i = 1; i < batchSize; i++) {
			// @formatter:off
			doSave(
					session,
					persister,
					image,
					manipulationContext.resolveCompressionName(id, compressionPrefixes[i]),
					contentArray[i]);
			// @formatter:on
		}

		return id;
	}

	private String doSave(FileResourceSession session, FileResourcePersister persister, Image originalImage, String id,
			byte[] content) throws Exception {
		// @formatter:off
		return super.save(
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
					.get(),
				session);
		// @formatter:on
	}

}