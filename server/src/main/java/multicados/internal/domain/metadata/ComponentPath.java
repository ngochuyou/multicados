/**
 * 
 */
package multicados.internal.domain.metadata;

import java.util.Queue;

public interface ComponentPath {

	void add(String component);

	Queue<String> getNodeNames();
	
}