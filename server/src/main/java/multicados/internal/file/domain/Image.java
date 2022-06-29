/**
 *
 */
package multicados.internal.file.domain;

import java.awt.image.BufferedImage;

import multicados.internal.file.engine.image.Standard;

/**
 * @author Ngoc Huy
 *
 */
public interface Image extends FileResource {

	void setStandard(Standard standard);

	Standard getStandard();

	void setBufferedImage(BufferedImage bufferedImage);

	BufferedImage getBufferedImage();

}
