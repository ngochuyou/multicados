/**
 *
 */
package multicados.internal.file.engine.image;

import static multicados.internal.helper.Utils.declare;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.event.spi.SaveOrUpdateEventListener;

import multicados.internal.config.Settings;
import multicados.internal.file.domain.FileResource;
import multicados.internal.file.domain.Image;
import multicados.internal.helper.StringHelper;
import multicados.internal.locale.ZoneContext;

/**
 * @author Ngoc Huy
 *
 */
public class IdentifierGeneratingSaveEventListener implements SaveOrUpdateEventListener {

	private static final long serialVersionUID = 1L;

	private static final String IDENTIFIER_TEMPLATE = "%s%s%s";

	private final ManipulationContext manipulationContext;
	private final ZoneContext zoneContext;
	private final String delimiter;
	private final int identifierLength;

	public IdentifierGeneratingSaveEventListener(ConfigurationService configurationService,
			ManipulationContext manipulationContext, ZoneContext zoneContext) {
		this.manipulationContext = manipulationContext;
		this.zoneContext = zoneContext;
		delimiter = configurationService.getSettings().get(Settings.FILE_RESOURCE_IDENTIFIER_DELIMITER).toString();
		identifierLength = Integer
				.valueOf(configurationService.getSettings().get(Settings.FILE_RESOURCE_IDENTIFIER_LENGTH).toString());
	}

	@Override
	public void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException {
		FileResource resource = (FileResource) event.getObject();

		try {
			if (Image.class.isAssignableFrom(resource.getClass())) {
				final Image image = (Image) resource;
				// @formatter:off
				declare(image.getContent())
					.then(ByteArrayInputStream::new)
					.then(ImageIO::read)
					.consume(image::setBufferedImage)
					.then(manipulationContext::resolveStandard)
					.consume(image::setStandard);
				// @formatter:on
				resource.setId(generateForImageResource(image));
				return;
			}

			resource.setId(generateForGenericResource(resource));
		} catch (Exception any) {
			throw new HibernateException(any);
		} finally {
			event.setRequestedId(resource.getId());
		}
	}

	private String generateForImageResource(FileResource resource) {
		final Image image = (Image) resource;

		return String.format(IDENTIFIER_TEMPLATE, image.getStandard().getName(), delimiter,
				generateForGenericResource(image));
	}

	private String generateForGenericResource(FileResource resource) {
		// @formatter:off
		try {
			return declare(LocalDateTime.now().atZone(zoneContext.getZone()).toInstant().toEpochMilli())
					.then(String::valueOf)
					.then(StringBuilder::new)
					.then(builder -> builder.append(delimiter))
					.then(builder -> builder.append(RandomStringUtils.randomAlphanumeric(identifierLength - builder.length() - resource.getExtension().length() - 1)))
					.then(builder -> builder.append(StringHelper.DOT))
					.then(builder -> builder.append(resource.getExtension()))
					.then(Object::toString)
					.get();
		} catch (Exception any) {
			any.printStackTrace();
			return null;
		}
		// @formatter:on
	}

}
