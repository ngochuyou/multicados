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

	public static final String SECURITY_PUBLIC_END_POINTS = "multicados.security.endpoints.public";

	public static final String SECURITY_TOKEN_END_POINT = "multicados.security.endpoints.token";

	public static final String SECURITY_JWT_HEADER_PREFIX = "multicados.security.jwt.headerprefix";

	public static final String SECURITY_JWT_COOKIE_NAME = "multicados.security.jwt.cookiename";
	
	public static final String SECURITY_JWT_SECRET = "multicados.security.jwt.secret";
	
	public static final String SECURITY_JWT_ZONE = "multicados.security.jwt.zone";
	
	public static final String SECURITY_JWT_EXPIRATION_DURATION = "multicados.security.jwt.expiration";
	
}
