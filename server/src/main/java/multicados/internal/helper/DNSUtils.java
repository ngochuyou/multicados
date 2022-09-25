/**
 * 
 */
package multicados.internal.helper;

import static multicados.internal.helper.Utils.declare;

import java.net.DatagramSocket;
import java.net.InetAddress;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author Ngoc Huy
 *
 */
@Component
public class DNSUtils {

	private final String hostAddress;
	private final int port;

	public DNSUtils(Environment env) throws Exception {
		final DatagramSocket socket = declare(new DatagramSocket())
				.consume(self -> self.connect(InetAddress.getByName("8.8.8.8"), 10002)).get();

		hostAddress = socket.getLocalAddress().getHostAddress();
		port = Integer.parseInt(env.getRequiredProperty("server.port"));
		socket.close();
	}

	public String getHostAddress() {
		return hostAddress;
	}

	public String getHostEndpoint() {
		return String.format("%s:%s", hostAddress, port);
	}

}
