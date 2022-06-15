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
import org.springframework.util.Assert;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextManager;
import multicados.internal.domain.Image;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.SpringHelper;

/**
 * @author Ngoc Huy
 *
 */
public class ImageService implements Service {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

	public static final String EXECUTOR_NAME = "image-service-excutor-";

	private static final Dimension DEFAULT_RESOLUTION = new Dimension(1366, 716);
	private static final Float[] DEFAULT_COMPRESSION_QUALITIES = new Float[] { 0.75f, 0.45f, 0.25f };
	private static final Float[] DEFAULT_COMPRESSION_FACTORS = new Float[] { 0.75f, 0.45f, 0.074f };

	private static final String DIMENSION_DELIMITER = "x";

	private final int propagationBatchSize;
	private final int originalWidth;
	private final int originalHeight;
	private final float originalAspectRatio;
	private final float[] compressionQualities;
	private final float[] compressionFactors;

	private final PropagationWorker worker;

	public ImageService(Environment env) throws Exception {
		Dimension originalDimension = locateOriginalResolution(env);

		originalWidth = Double.valueOf(originalDimension.getWidth()).intValue();
		originalHeight = Double.valueOf(originalDimension.getHeight()).intValue();
		originalAspectRatio = Double.valueOf((originalWidth * 1.0) / (originalHeight * 1.0)).floatValue();

		compressionQualities = SpringHelper.getFloatsOrDefault(env, Settings.FILE_RESOURCE_IMAGE_COMPRESSION_QUALITIES,
				DEFAULT_COMPRESSION_QUALITIES);
		compressionFactors = SpringHelper.getFloatsOrDefault(env, Settings.FILE_RESOURCE_IMAGE_COMPRESSION_FACTORS,
				DEFAULT_COMPRESSION_FACTORS);
		Assert.isTrue(compressionQualities.length == compressionFactors.length,
				String.format("%s and %s must have the same length", Settings.FILE_RESOURCE_IMAGE_COMPRESSION_QUALITIES,
						Settings.FILE_RESOURCE_IMAGE_COMPRESSION_FACTORS));

		propagationBatchSize = compressionQualities.length;

		for (int i = 0; i < propagationBatchSize; i++) {
			Assert.isTrue(compressionQualities[i] > 0, String.format("%s must not contain negative values",
					Settings.FILE_RESOURCE_IMAGE_COMPRESSION_QUALITIES));
			Assert.isTrue(compressionFactors[i] > 0, String.format("%s must not contain negative values",
					Settings.FILE_RESOURCE_IMAGE_COMPRESSION_FACTORS));
		}

		worker = instantiatePropagationWorker();
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

	private Dimension locateOriginalResolution(Environment env) throws Exception {
		// @formatter:off
		return SpringHelper.getOrDefault(
					env,
					Settings.FILE_RESOURCE_IMAGE_ORIGINAL_RESOLUTION,
					value -> declare(value)
						.then(val -> val.split(DIMENSION_DELIMITER))
							.flat(parts -> Integer.valueOf(parts[0]), parts -> Integer.valueOf(parts[1]))
						.consume((width, height) -> Assert.isTrue(width * height >= 0, String.format("%s must not be negative", Settings.FILE_RESOURCE_IMAGE_ORIGINAL_RESOLUTION)))
						.then((width, height) -> new Dimension(width, height))
						.get(),
					DEFAULT_RESOLUTION);
		// @formatter:on
	}

	public byte[][] adjustAndPropagate(Image image) throws IOException, InterruptedException, ExecutionException {
		byte[][] adjustmentProduct = new byte[][] { adjust(image.getContent(), image.getExtension()) };
		byte[][] propagationProducts = propagate(adjustmentProduct[0], image.getExtension());

		return CollectionHelper.join(byte[].class, adjustmentProduct, propagationProducts);
	}

	private byte[] adjust(byte[] requestedBytes, String extension) throws IOException {
		BufferedImage requestedImage = ImageIO.read(new ByteArrayInputStream(requestedBytes));
		Dimension refinedDimension = refine(requestedImage);

		if (logger.isDebugEnabled()) {
			logger.debug("Refined request dimension: {}", refinedDimension);
		}

		BufferedImage adjustedImage = ImageUtils.adjustResolution(requestedImage, extension,
				(int) refinedDimension.getWidth(), (int) refinedDimension.getHeight());
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		ImageIO.write(adjustedImage, extension, output);

		output.close();

		return output.toByteArray();
	}

	private Dimension refine(BufferedImage requestedBuffer) {
		int requestedWidth = requestedBuffer.getWidth();

		if (requestedWidth > originalWidth) {
			return new Dimension(originalWidth, originalHeight);
		}

		return new Dimension(requestedWidth,
				Double.valueOf(Math.ceil(requestedWidth / originalAspectRatio)).intValue());
	}

	private byte[][] propagate(byte[] requestedBytes, String extension)
			throws IOException, InterruptedException, ExecutionException {
		byte[][] products = new byte[propagationBatchSize][];
		@SuppressWarnings("unchecked")
		CompletableFuture<byte[]>[] futures = new CompletableFuture[propagationBatchSize];
		BufferedImage originalBuffer = ImageIO.read(new ByteArrayInputStream(requestedBytes));
		int originalWidth = originalBuffer.getWidth();
		int originalHeight = originalBuffer.getHeight();

		for (int i = 0; i < propagationBatchSize; i++) {
			futures[i] = worker.propagate(originalBuffer, extension, (int) (originalWidth * compressionFactors[i]),
					(int) (originalHeight * compressionFactors[i]), compressionQualities[i]);
		}

		CompletableFuture.allOf(futures).join();

		for (int i = 0; i < propagationBatchSize; i++) {
			products[i] = futures[i].get();
		}

		return products;
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
