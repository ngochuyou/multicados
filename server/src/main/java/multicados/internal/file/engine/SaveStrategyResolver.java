/**
 * 
 */
package multicados.internal.file.engine;

import java.util.Map;

import org.hibernate.service.Service;

import multicados.internal.file.domain.FileResource;
import multicados.internal.file.domain.Image;
import multicados.internal.file.engine.image.ImageSaveStrategy;
import multicados.internal.file.engine.image.ImageService;
import multicados.internal.file.engine.image.ManipulationContext;

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
		return Image.class.isAssignableFrom(resourceType) ? saveStrategies.get(Image.class)
				: saveStrategies.get(FileResource.class);
	}

}
