/**
 *
 */
package multicados.internal.file.engine.image;

import java.awt.image.BufferedImage;
import java.util.List;

import org.hibernate.service.Service;

/**
 * @author Ngoc Huy
 *
 */
public interface ManipulationContext extends Service {

	Standard locateStandard(String filename);

	Standard resolveStandard(BufferedImage bufferedImage);

	List<Standard> getStandards();

	String resolveCompressionName(String filename, String prefix);

	int getMaximumIdentifierOccupancy();

}
