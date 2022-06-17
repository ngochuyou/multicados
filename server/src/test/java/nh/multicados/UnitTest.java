/**
 * 
 */
package nh.multicados;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;

/**
 * @author Ngoc Huy
 *
 */
public class UnitTest {

	public static void main(String[] args) throws Exception {
		String filePath = "C:\\Users\\Ngoc Huy\\Pictures\\muticados\\user\\L_1655397664615_O105lZQ1.jpg";

		Thread blocker = new Thread(() -> {
			System.out.println(Thread.currentThread().getName());
			File file = new File(filePath);

			try {
				RandomAccessFile access = new RandomAccessFile(file, "rw");

				access.getChannel();

				FileLock lock = access.getChannel().lock();
				System.out.println("Locking");
				Thread.sleep(10000);
				System.out.println("Realeasing");
				lock.release();
				access.close();
			} catch (Exception any) {
				any.printStackTrace();
			}
		});
		Thread reader = new Thread(() -> {
			try {
				System.out.println(Thread.currentThread().getName());
				Thread.sleep(2000);
				System.out.println("Reading");
				Files.readAllBytes(new File(filePath).toPath());
				System.out.println("Read");
			} catch (Exception any) {
				any.printStackTrace();
			}
		});

		blocker.start();
		reader.start();
	}

}
