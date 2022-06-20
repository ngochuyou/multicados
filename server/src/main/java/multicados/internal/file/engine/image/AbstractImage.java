/**
 * 
 */
package multicados.internal.file.engine.image;

import java.awt.image.BufferedImage;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import multicados.internal.file.domain.AbstractFileResource;
import multicados.internal.file.domain.Image;

/**
 * @author Ngoc Huy
 *
 */
@MappedSuperclass
public abstract class AbstractImage extends AbstractFileResource implements Image {

	@Transient
	private Standard standard;

	@Transient
	private BufferedImage bufferedImage;

	@Override
	public Standard getStandard() {
		return standard;
	}

	@Override
	public void setStandard(Standard standard) {
		this.standard = standard;
	}

	public BufferedImage getBufferedImage() {
		return bufferedImage;
	}

	public void setBufferedImage(BufferedImage bufferedImage) {
		this.bufferedImage = bufferedImage;
	}

}
