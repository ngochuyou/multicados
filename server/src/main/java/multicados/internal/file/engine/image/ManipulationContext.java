/**
 * 
 */
package multicados.internal.file.engine.image;

import java.awt.image.BufferedImage;

/**
 * @author Ngoc Huy
 *
 */
public interface ManipulationContext {

	Standard getStandard(Ratio ratio);

	Standard resolveStandard(BufferedImage bufferedImage);

}
