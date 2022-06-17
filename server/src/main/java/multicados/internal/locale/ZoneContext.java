/**
 * 
 */
package multicados.internal.locale;

import java.time.ZoneId;

import org.hibernate.service.Service;

/**
 * @author Ngoc Huy
 *
 */
public interface ZoneContext extends Service {

	ZoneId getZone();

}
