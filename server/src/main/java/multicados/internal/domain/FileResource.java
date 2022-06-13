/**
 * 
 */
package multicados.internal.domain;

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
