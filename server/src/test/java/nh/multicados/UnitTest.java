/**
 * 
 */
package nh.multicados;

import multicados.internal.domain.metadata.AccessorFactory;
import multicados.internal.domain.metadata.AccessorFactory.Accessor;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class UnitTest {

	public static void main(String[] args) throws Exception {
		Parent obj = new Parent();
		Accessor accessor = AccessorFactory.standard(Parent.class, "size");
		
		Utils.declare(obj)
			.then(accessor::get)
			.identical(System.out::println);
		
		accessor.set(obj, Integer.valueOf(10));
		
		Utils.declare(obj)
			.then(accessor::get)
			.identical(System.out::println);
	}

	public static class Child {

		public String p = "hello";

	}

	public static class Parent {

		private int size;

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}

	}

}
