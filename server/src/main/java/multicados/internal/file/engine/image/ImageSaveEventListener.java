/**
 * 
 */
package multicados.internal.file.engine.image;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.event.spi.SaveOrUpdateEventListener;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.file.domain.Image;
import multicados.internal.file.engine.FileResourceSessionFactory;

/**
 * @author Ngoc Huy
 *
 */
public class ImageSaveEventListener implements SaveOrUpdateEventListener {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(ImageSaveEventListener.class);

	private final MetamodelImplementor metamodel;
	private final ImageService service;

	public ImageSaveEventListener(FileResourceSessionFactory sessionFactory, ImageService service) {
		this.metamodel = sessionFactory.getMetamodel();
		this.service = service;
	}

	@Override
	public void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException {
		Object entity = event.getObject();

		if (!Image.class.isAssignableFrom(entity.getClass())) {
			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Executing image services on {}", entity.getClass());
		}

		try {
			Image image = Image.class.cast(entity);
			byte[][] products = service.adjustAndPropagate(image).getSecond();

			image.setContent(products[0]);
		} catch (InterruptedException | IOException | ExecutionException any) {
			throw new HibernateException(any);
		}
//		source.getActionQueue().addAction(new entityinsertaction);

//		System.out.println(event.getRequestedId());
//		System.out.println(EntityState.getEntityState(event.getEntity(), event.getEntityName(), event.getEntry(),
//				event.getSession(), null));
	}

}
