/**
 * 
 */
package multicados.internal.config;

/**
 * @author Ngoc Huy
 *
 */
public interface Settings {

	public static final String BASE_PACKAGE = "multicados";

	public static final String SCANNED_ENTITY_PACKAGES = "multicados.scanned-packages.entity";

	public static final String READ_FAILURE_EXCEPTION_THROWING_STRATEGY = "multicados.crud.security.read.failurestrategy";

	public static final String DUMMY_DATABASE_MODE = "multicados.dummy-database.initializer";
	
	public static final String DUMMY_DATABASE_PATH = "multicados.dummy-database.path";
	
}
