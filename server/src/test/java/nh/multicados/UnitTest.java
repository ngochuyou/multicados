/**
 * 
 */
package nh.multicados;

import java.io.IOException;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Ngoc Huy
 *
 */
public class UnitTest {

	private void swap(int[] arr, int i, int j) {
		int t = arr[i];

		arr[i] = arr[j];
		arr[j] = t;
	}

	private void heapify(int[] arr, int n, int i) {
		int j = 2 * i + 1;

		if (j >= n) {
			return;
		}

		if (j + 1 < n) {
			if (arr[j + 1] > arr[j]) {
				j++;
			}
		}

		if (arr[i] >= arr[j]) {
			return;
		}

		swap(arr, i, j);
		// propagate
		heapify(arr, n, j);
	}

	@Test
	public void test() throws IOException {
		int n = 20;
		int[] arr = new int[n];

		for (int i = 0; i < n; i++) {
			arr[i] = RandomUtils.nextInt(0, 10);
		}

		show(arr);

		for (int i = n / 2 - 1; i >= 0; i--) {
			heapify(arr, n, i);
		}

		for (int i = n - 1; i >= 0; i--) {
			swap(arr, 0, i);
			heapify(arr, i, 0);
		}

		show(arr);
	}

	private void show(int[] arr) {
		for (int i = 0; i < arr.length; i++) {
			System.out.print(arr[i] + "\s");
		}

		System.out.println();
	}

}
