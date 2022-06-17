/**
 * 
 */
package multicados.internal.locale;

import static org.springframework.util.StringUtils.hasLength;

import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import multicados.internal.config.Settings;

/**
 * @author Ngoc Huy
 *
 */
@Component
public class ZoneContextImpl implements ZoneContext {

	/**
	 * 
	 */
	public static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(ZoneContextImpl.class);

	private static final String LOCAL_ZONE = "LOCAL";
	private final ZoneId zoneId;

	public ZoneContextImpl(Environment env) {
		this.zoneId = locateZone(env);
		logger.debug("Using ZoneId: {}", zoneId.getId());
	}

	private ZoneId locateZone(Environment env) {
		String configuredZone = env.getProperty(Settings.ZONE);

		if (!hasLength(configuredZone) || LOCAL_ZONE.equals(configuredZone.toUpperCase())) {
			return ZoneId.systemDefault();
		}

		if (!ZoneId.SHORT_IDS.containsKey(configuredZone)) {
			throw new IllegalArgumentException(String.format("Unknown zone id [%s]", configuredZone));
		}

		return ZoneId.of(ZoneId.SHORT_IDS.get(configuredZone));
	}

	@Override
	public ZoneId getZone() {
		return zoneId;
	}

}
