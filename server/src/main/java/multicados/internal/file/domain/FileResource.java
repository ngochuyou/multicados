/**
 *
 */
package multicados.internal.file.domain;

import multicados.internal.domain.IdentifiableResource;

/**
 * @author Ngoc Huy
 *
 */
public interface FileResource extends IdentifiableResource<String> {

	byte[] getContent();

	void setContent(byte[] content);

	String getExtension();

	void setExtension(String extension);

}
