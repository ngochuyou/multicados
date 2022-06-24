/**
 * 
 */
package multicados.internal.security;

import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author Ngoc Huy
 *
 */
public class OnMemoryUserDetailsContextImpl implements OnMemoryUserDetailsContext {

	private static final Logger logger = LoggerFactory.getLogger(OnMemoryUserDetailsContextImpl.class);

	private final MutexMap mutecies = new MutexMap();

	@Override
	public void put(UserDetails userDetails) {
		String username = userDetails.getUsername();

		if (mutecies.containsKey(username)) {
			return;
		}

		Object lock = new Object();

		synchronized (lock) {
			mutecies.put(username, new Mutex(userDetails, lock));

			if (logger.isDebugEnabled()) {
				logger.debug("Entry: {}", username);
			}
		}
	}

	@Override
	public UserDetails get(String username) {
		if (!mutecies.containsKey(username)) {
			return null;
		}

		return mutecies.get(username).userDetails;
	}

	@SuppressWarnings("unused")
	private void remove(String username) {
		if (!mutecies.containsKey(username)) {
			return;
		}

		synchronized (mutecies.get(username).lock) {
			Mutex mutex = mutecies.remove(username);

			if (logger.isDebugEnabled()) {
				logger.debug("Exit: {}", username);
			}
		}
	}

	private class MutexMap extends LinkedHashMap<String, Mutex> {

		private static final long serialVersionUID = 1L;

		private static final int MAX_SIZE = 1000;

		public MutexMap() {
			super(MAX_SIZE, 1.5f);
		}

		@Override
		protected synchronized boolean removeEldestEntry(java.util.Map.Entry<String, Mutex> eldest) {
			return size() == MAX_SIZE;
		}

	}

	private class Mutex {

		final UserDetails userDetails;
		final Object lock;

		public Mutex(UserDetails userDetails, Object lock) {
			this.userDetails = userDetails;
			this.lock = lock;
		}

	}

}
