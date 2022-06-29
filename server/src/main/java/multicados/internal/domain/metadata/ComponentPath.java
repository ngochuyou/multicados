/**
 *
 */
package multicados.internal.domain.metadata;

import java.util.Queue;

import multicados.internal.domain.DomainComponent;

/**
 * Contains the full path to an attribute of a {@link DomainComponent}
 *
 * @author Ngoc Huy
 *
 */
public interface ComponentPath {

	/**
	 * Add a node to the current path
	 *
	 * @param component
	 */
	void add(String component);

	/**
	 * @return the full path as a {@link Queue}
	 */
	Queue<String> getPath();

}