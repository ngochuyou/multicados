/**
 * 
 */
package nh.multicados;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ngoc Huy
 *
 */
public class UnitTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		A a = new A();
		Map<String, String> b = new HashMap<>();
		
		b.put("b", "world");
		a.and(b);
		a.print();
		b.put("b", "my friend");
	}
	
	private static class A {
		
		private Map<String, String> eles;
		
		public A() {
			eles = new HashMap<>();
			
			eles.put("a", "hello");
		}
		
		void and(Map<String, String> other) {
			eles.putAll(other);
		}
		
		void print() {
			eles.values().stream().forEach(System.out::println);
		}
		
	}

}
