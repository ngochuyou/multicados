/**
 * 
 */
package multicados.internal.file.domain;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.GenericGenerator;

import multicados.internal.file.engine.FileManagementImpl.FileIdentifierGenerator;

/**
 * @author Ngoc Huy
 *
 */
@MappedSuperclass
public abstract class AbstractFileResource implements FileResource {

	@Id
	@GeneratedValue(generator = FileIdentifierGenerator.NAME)
	@GenericGenerator(strategy = FileIdentifierGenerator.NAME, name = FileIdentifierGenerator.NAME)
	private String id;

	private byte[] content;

	private String extension;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public byte[] getContent() {
		return content;
	}

	@Override
	public void setContent(byte[] content) {
		this.content = content;
	}

	@Override
	public String getExtension() {
		return extension;
	}

	@Override
	public void setExtension(String extension) {
		this.extension = extension;
	}

}
