/**
 *
 */
package multicados.internal.domain.metadata;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.stream.Collectors;

import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
public class ComponentPathImpl implements ComponentPath {

	private final Queue<String> path;

	public ComponentPathImpl() {
		path = new ArrayDeque<>();
	}

	public ComponentPathImpl(String root) {
		path = new ArrayDeque<>();
		path.add(root);
	}

	public ComponentPathImpl(ComponentPathImpl parent) {
		this();
		path.addAll(parent.path);
	}

	@Override
	public void add(String component) {
		path.add(component);
	}

	@Override
	public Queue<String> getPath() {
		return path;
	}

	@Override
	public String toString() {
		return path.stream().collect(Collectors.joining(StringHelper.DOT));
	}
}