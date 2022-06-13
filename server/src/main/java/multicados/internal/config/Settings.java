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

	public static final String ACTIVE_PROFILES = "spring.profiles.active";

	public static final String ZONE = "multicados.zone";

	public static final String SCANNED_ENTITY_PACKAGES = "multicados.scanned-packages.entity";
	public static final String SCANNED_FILE_RESOURCE_PACKAGE = "multicados.scanned-packages.file";
	
	public static final String READ_FAILURE_EXCEPTION_THROWING_STRATEGY = "multicados.crud.security.read.failurestrategy";

	public static final String DUMMY_DATABASE_MODE = "multicados.dummy-database.initializer";
	public static final String DUMMY_DATABASE_PATH = "multicados.dummy-database.path";

	public static final String SECURITY_PUBLIC_END_POINTS = "multicados.security.endpoints.public";
	public static final String SECURITY_JWT_TOKEN_END_POINT = "multicados.security.jwt.token.endpoint";
	public static final String SECURITY_JWT_TOKEN_USERNAME = "multicados.security.jwt.param.username";
	public static final String SECURITY_JWT_TOKEN_PASSWORD = "multicados.security.jwt.param.password";
	public static final String SECURITY_JWT_HEADER_PREFIX = "multicados.security.jwt.headerprefix";
	public static final String SECURITY_JWT_COOKIE_NAME = "multicados.security.jwt.cookiename";
	public static final String SECURITY_JWT_SECRET = "multicados.security.jwt.secret";
	public static final String SECURITY_JWT_EXPIRATION_DURATION = "multicados.security.jwt.expiration";
	public static final String SECURITY_DEV_CLIENT_PORTS = "multicados.security.dev.client.ports";

	public static final String FILE_RESOURCE_ROOT_DIRECTORY = "multicados.file.directory";

	public static final String DEFAULT_PRODUCTION_PROFILE = "PROD";
	public static final String DEAULT_FILE_RESOURCE_ROOT_DIRECTORY = "files\\";
	
}
