/**
 *
 */
package multicados.internal.file.engine;

import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		final Logger logger = LoggerFactory.getLogger(SaveStrategyResolver.class);

		if (logger.isTraceEnabled()) {
			logger.trace("Instantiating {}", SaveStrategyResolver.class.getName());
		}

		saveStrategies = Map.of(FileResource.class, new DefaultSaveStrategy(), Image.class,
				new ImageSaveStrategy(imageService, manipulationContext));

		if (logger.isDebugEnabled()) {
			logger.debug("Configured {} are:\n\t{}", SaveStrategyResolver.class.getName(),
					saveStrategies.entrySet().stream().map(
							entry -> String.format("%s:\t%s", entry.getKey(), entry.getValue().getClass().getName()))
							.collect(Collectors.joining("\n\t")));
		}
	}

	public <T extends FileResource> SaveStrategy getSaveStrategy(Class<T> resourceType) {
		return Image.class.isAssignableFrom(resourceType) ? saveStrategies.get(Image.class)
				: saveStrategies.get(FileResource.class);
	}

}
