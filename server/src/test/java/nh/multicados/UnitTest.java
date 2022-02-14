/**
 * 
 */
package nh.multicados;

import java.util.stream.Stream;

/**
 * @author Ngoc Huy
 *
 */
public class UnitTest {

	public static void main(String[] args) throws Exception {
		Stream.of(1, 2, 3).reduce((total, current) -> {
			return current + total;
		});
	}

}
