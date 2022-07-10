/**
 * 
 */
package multicados.internal.file.engine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ngoc Huy
 *
 */
public class DirectoryInitializerImpl implements DirectoryInitializer {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(DirectoryInitializerImpl.class);

	@Override
	public void createDirectory(String directory) {
		Path path = Paths.get(directory);

		if (!Files.exists(path)) {
			logger.debug("Creating new directory with path [{}]", path);
			path.toFile().mkdir();
			return;
		}

		if (Files.isDirectory(path)) {
			return;
		}

		throw new IllegalArgumentException(String.format("%s has already existed but it's not a directory", path));
	}

}
