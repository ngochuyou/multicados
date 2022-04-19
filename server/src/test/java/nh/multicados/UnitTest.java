/**
 * 
 */
package nh.multicados;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author Ngoc Huy
 *
 */
public class UnitTest {

	public static void main(String[] args) throws Exception {
		Queue<String> queue = new ArrayDeque<>();

		queue.add("abc");
		queue.add("def");
		queue.add("ghi");

		while (queue.size() > 1) {
			System.out.println(queue.poll());
		}

		System.out.println(queue.element());
	}

}
