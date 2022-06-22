/**
 * 
 */
package multicados.internal.file.engine.image;

import static multicados.internal.helper.Utils.declare;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.hibernate.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;

import multicados.internal.file.domain.Image;
import multicados.internal.helper.AwtImageUtils;
import multicados.internal.helper.Utils.BiDeclaration;

/**
 * @author Ngoc Huy
 *
 */
public class ImageService implements Service {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

	public static final String EXECUTOR_NAME = "img-manipulation-";

	private final PropagationWorker worker;

	public ImageService(ApplicationContext applicationContext, Environment env) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Instantiating {}", ImageService.class.getName());
		}

		worker = instantiatePropagationWorker(applicationContext);
	}

	private PropagationWorker instantiatePropagationWorker(ApplicationContext applicationContext) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Registering {} as a Spring Bean", PropagationWorker.class.getName());
		}
		// @formatter:off
		return declare(applicationContext, PropagationWorker.class, new GenericBeanDefinition())
				.consume((appContext, type, bean) -> bean.setLazyInit(false))
				.consume((appContext, type, bean) -> bean.setBeanClass(type))
				.consume((appContext, type, bean) -> BeanDefinitionRegistry.class.cast(appContext.getAutowireCapableBeanFactory()).registerBeanDefinition(type.getName(), bean))
			.useSecond()
			.then(applicationContext::getBean)
			.get();
		// @formatter:on
	}

	public BiDeclaration<Standard, byte[][]> adjustAndPropagate(Image image) throws Exception {
		return new AdjustmentAndPropagation(image).doWork();
	}

	private class AdjustmentAndPropagation {

		private final Image image;
		private final Standard standard;

		private final byte[][] products;

		public AdjustmentAndPropagation(Image image) throws IOException {
			this.image = image;
			standard = image.getStandard();
			products = new byte[standard.getBatchSize()][];
		}

		private BiDeclaration<Standard, byte[][]> doWork() throws Exception {
			Dimension refinedDimension = refineDimension();

			if (logger.isDebugEnabled()) {
				logger.debug("Refined request dimension: {}", refinedDimension);
			}
			// @formatter:off
			BufferedImage adjustedImage =
					declare(AwtImageUtils.adjustResolution(image.getBufferedImage(), image.getExtension(), (int) refinedDimension.getWidth(), (int) refinedDimension.getHeight(), (image, extension) -> image))
							.second(image.getExtension())
							.third(standard.getCompressionFactors()[0])
						.then((image, extension, factor) -> AwtImageUtils.downGradeQuality(image, extension, factor, (bytes, any) -> bytes))
						.consume(bytes -> products[0] = bytes)
						.then(ByteArrayInputStream::new)
						.then(ImageIO::read)
						.get();
			// @formatter:on
			propagate(adjustedImage);

			return declare(standard, products);
		}

		private Dimension refineDimension() {
			int requestedWidth = image.getBufferedImage().getWidth();

			if (requestedWidth > standard.getOriginalWidth()) {
				return new Dimension(standard.getOriginalWidth(), standard.getOriginalHeight());
			}

			return new Dimension(requestedWidth, standard.maintainHeight(requestedWidth));
		}

		@Async
		private void propagate(BufferedImage toBePropagatedImage) throws Exception {
			int propagationBatchSize = standard.getBatchSize() - 1;
			@SuppressWarnings("unchecked")
			CompletableFuture<byte[]>[] futures = new CompletableFuture[propagationBatchSize];
			int originalWidth = toBePropagatedImage.getWidth();

			for (int i = 0, j = i + 1; i < propagationBatchSize; i++, j++) {
				int nextWidth = (int) (originalWidth * standard.getCompressionFactors()[j]);
				int nextHeight = standard.maintainHeight(nextWidth);

				futures[i] = worker.propagate(toBePropagatedImage, image.getExtension(), nextWidth, nextHeight,
						standard.getCompressionQualities()[j]);
			}

			CompletableFuture.allOf(futures).join();

			for (int i = 0; i < propagationBatchSize; i++) {
				products[i + 1] = futures[i].get();
			}
		}
	}

	// has to be static
	public static class PropagationWorker {

		@Async(EXECUTOR_NAME)
		public CompletableFuture<byte[]> propagate(BufferedImage originalImage, String extension, int nextWidth,
				int nextHeight, float qualityFactor) throws Exception {
			if (logger.isDebugEnabled()) {
				logger.debug("Executing propagation with: width:{}, height:{}, quality factor:{}", nextWidth,
						nextHeight, qualityFactor);
			}
			// @formatter:off
			return CompletableFuture.completedFuture(
					AwtImageUtils.downGradeQuality(
						AwtImageUtils.adjustResolution(
							originalImage,
							extension,
							nextWidth,
							nextHeight,
							(image, any) -> image),
						extension,
						qualityFactor,
						(bytes, any) -> bytes));
			// @formatter:on
		}

	}

}
