/**
 * 
 */
package multicados.internal.file.engine;

import static multicados.internal.helper.Utils.declare;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.file.domain.FileResource;

public class DefaultSaveStrategy implements SaveStrategy {

	private static final Logger logger = LoggerFactory.getLogger(DefaultSaveStrategy.class);

	private void logInsert(Path path, byte[] content) {
		if (logger.isDebugEnabled()) {
			logger.debug("Writing file [{}] with content length {}", path, content.length);
		}
	}

	@Override
	public <T extends FileResource> String save(FileResourcePersister persister, String id, T file,
			FileResourceSession session) throws Exception {
		// @formatter:off
		declare(id)
			.then(persister::resolvePath)
			.then(File::new)
			.then(File::getPath)
			.then(Paths::get)
				.second(file.getContent())
			.consume(this::logInsert)
			.consume(Files::write);
		// @formatter:on
		return id;
	}

}