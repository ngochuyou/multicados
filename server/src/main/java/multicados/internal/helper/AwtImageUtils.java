/**
 *
 */
package multicados.internal.helper;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import multicados.internal.helper.Utils.HandledBiFunction;

public class AwtImageUtils {

	private AwtImageUtils() {
		throw new UnsupportedOperationException();
	}
	
	public static byte[] getBytes(BufferedImage bufferedImage, String extension) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		ImageIO.write(bufferedImage, extension, baos);
		baos.close();

		return baos.toByteArray();
	}

	public static <T> T adjustResolution(
	// @formatter:off
			BufferedImage original,
			String extension,
			int nextWidth,
			int nextHeight,
			HandledBiFunction<BufferedImage, String, T, Exception> producer) throws Exception {
		// @formatter:on
		final BufferedImage resized = new BufferedImage(nextWidth, nextHeight, original.getType());
		final Graphics2D graphics = resized.createGraphics();

		graphics.drawImage(original, 0, 0, nextWidth, nextHeight, null);
		graphics.dispose();

		return producer.apply(resized, extension);
	}

	public static <T> T downGradeQuality(BufferedImage toBeDownGraded, String extension, float factor,
			HandledBiFunction<byte[], String, T, Exception> producer) throws Exception {
		final ImageWriter writer = ImageWriter.class.cast(ImageIO.getImageWritersByFormatName(extension).next());
		final ImageWriteParam param = writer.getDefaultWriteParam();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ImageOutputStream ios = new MemoryCacheImageOutputStream(baos);

		try {
			writer.setOutput(ios);
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(factor); // Change the quality value you prefer
			writer.write(null, new IIOImage(toBeDownGraded, null, null), param);

			return producer.apply(baos.toByteArray(), extension);
		} finally {
			ios.close();
			baos.close();
			writer.dispose();
		}
	}

	public static byte[] adjustAndDownGrade(byte[] originalBytes, String extension, int nextWidth, int nextHeight)
			throws Exception {
		// @formatter:off
		return downGradeQuality(
				adjustResolution(ImageIO.read(new ByteArrayInputStream(originalBytes)), extension, nextWidth, nextHeight, (image, any) -> image),
				extension,
				nextHeight,
				(bytes, any) -> bytes);
		// @formatter:on
	}

}