/**
 * 
 */
package multicados.internal.file.engine.image;

import java.awt.image.BufferedImage;

import org.hibernate.service.Service;

/**
 * @author Ngoc Huy
 *
 */
public interface ManipulationContext extends Service {

	Standard getStandard(Ratio ratio);

	Standard resolveStandard(BufferedImage bufferedImage);
	
	String resolveCompressionName(String filename, String prefix);

}
