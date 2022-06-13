/**
 * 
 */
package multicados.internal.file.engine;

import multicados.internal.context.ContextBuilder;

/**
 * @author Ngoc Huy
 *
 */
public interface FileManagement extends ContextBuilder {

	FileResourceSessionFactory getSessionFactory();

}
