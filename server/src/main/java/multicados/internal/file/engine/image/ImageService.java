/**
 * 
 */
package multicados.internal.file.engine.image;

import static multicados.internal.helper.Utils.declare;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.hibernate.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;

import multicados.internal.context.ContextManager;
import multicados.internal.file.domain.Image;
import multicados.internal.helper.Utils.BiDeclaration;

/**
 * @author Ngoc Huy
 *
 */
public class ImageService implements Service {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

	public static final String EXECUTOR_NAME = "image-manipulation-";

	private final ManipulationContext manipulationContext;
	private final PropagationWorker worker;

	public ImageService(Environment env, ManipulationContext manipulationContext) throws Exception {
		worker = instantiatePropagationWorker();
		this.manipulationContext = manipulationContext;
	}

	private PropagationWorker instantiatePropagationWorker() throws Exception {
		// @formatter:off
		return declare(new GenericBeanDefinition())
			.consume(bean -> bean.setLazyInit(false))
			.consume(bean -> bean.setBeanClass(PropagationWorker.class))
				.prepend(PropagationWorker.class)
			.consume(ContextManager::registerBean)
			.useFirst()
			.then(ContextManager::getBean)
			.get();
		// @formatter:on
	}

	public BiDeclaration<Standard, byte[][]> adjustAndPropagate(Image image)
			throws IOException, InterruptedException, ExecutionException {
		return new AdjustmentAndPropagation(image).doWork();
	}

	private class AdjustmentAndPropagation {

		private final Image image;
		private BufferedImage originalBufferedImage;
		private final Standard standard;

		private final byte[][] products;

		public AdjustmentAndPropagation(Image image) throws IOException {
			this.image = image;
			originalBufferedImage = ImageIO.read(new ByteArrayInputStream(image.getContent()));
			standard = manipulationContext.resolveStandard(originalBufferedImage);
			products = new byte[standard.getBatchSize()][];
		}

		private BiDeclaration<Standard, byte[][]> doWork()
				throws IOException, InterruptedException, ExecutionException {
			Dimension refinedDimension = refineDimension();

			if (logger.isDebugEnabled()) {
				logger.debug("Refined request dimension: {}", refinedDimension);
			}

			BufferedImage adjustedImage = ImageUtils.adjustResolution(originalBufferedImage, image.getExtension(),
					(int) refinedDimension.getWidth(), (int) refinedDimension.getHeight());
			ByteArrayOutputStream output = new ByteArrayOutputStream();

			try {
				originalBufferedImage = null;
				ImageIO.write(adjustedImage, image.getExtension(), output);
				products[0] = output.toByteArray();
			} finally {
				output.close();
			}

			propagate(adjustedImage);

			return declare(standard, products);
		}

		private Dimension refineDimension() {
			int requestedWidth = originalBufferedImage.getWidth();

			if (requestedWidth > standard.getOriginalWidth()) {
				return new Dimension(standard.getOriginalWidth(), standard.getOriginalHeight());
			}

			return new Dimension(requestedWidth, standard.maintainHeight(requestedWidth));
		}

		private void propagate(BufferedImage toBePropagatedImage)
				throws IOException, InterruptedException, ExecutionException {
			int propagationBatchSize = standard.getBatchSize() - 1;
			@SuppressWarnings("unchecked")
			CompletableFuture<byte[]>[] futures = new CompletableFuture[propagationBatchSize];
			int originalWidth = toBePropagatedImage.getWidth();

			for (int i = 0; i < propagationBatchSize; i++) {
				int nextWidth = (int) (originalWidth * standard.getCompressionFactors()[i]);
				int nextHeight = standard.maintainHeight(nextWidth);

				futures[i] = worker.propagate(toBePropagatedImage, image.getExtension(), nextWidth, nextHeight,
						standard.getCompressionQualities()[i]);
			}

			CompletableFuture.allOf(futures).join();

			for (int i = 1; i < propagationBatchSize; i++) {
				products[i] = futures[i - 1].get();
			}
		}
	}

	private interface ImageUtils {

		public static BufferedImage adjustResolution(BufferedImage original, String extension, int nextWidth,
				int nextHeight) throws IOException {
			BufferedImage resized = new BufferedImage(nextWidth, nextHeight, original.getType());
			Graphics2D graphics = resized.createGraphics();

			graphics.drawImage(original, 0, 0, nextWidth, nextHeight, null);
			graphics.dispose();

			return resized;
		}

		public static byte[] downGradeQuality(BufferedImage toBeDownGraded, String extension, float factor)
				throws IOException {
			ImageWriter writer = ImageWriter.class.cast(ImageIO.getImageWritersByFormatName(extension).next());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
			ImageWriteParam param = writer.getDefaultWriteParam();

			try {
				writer.setOutput(ios);
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(factor); // Change the quality value you prefer
				writer.write(null, new IIOImage(toBeDownGraded, null, null), param);

				return baos.toByteArray();
			} finally {
				baos.close();
				ios.close();
				writer.dispose();
			}
		}

	}

	// has to be static
	public static class PropagationWorker {

		@Async(EXECUTOR_NAME)
		public CompletableFuture<byte[]> propagate(BufferedImage originalImage, String extension, int nextWidth,
				int nextHeight, float qualityFactor) throws IOException {
			if (logger.isDebugEnabled()) {
				logger.debug("Executing propagation in {}, info: {} x {} x {}", Thread.currentThread().getName(),
						nextWidth, nextHeight, qualityFactor);
			}

			return CompletableFuture.completedFuture(ImageUtils.downGradeQuality(
					ImageUtils.adjustResolution(originalImage, extension, nextWidth, nextHeight), extension,
					qualityFactor));
		}

	}

}
