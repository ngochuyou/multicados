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
	public static final String INTERNAL_BASE_PACKAGE = "multicados.internal";
	public static final String DOMAIN_SPECIFIC_BASE_PACKAGE = "multicados.domain";

	public static final String ACTIVE_PROFILES = "spring.profiles.active";

	public static final String ZONE = "multicados.zone";

	public static final String SCANNED_ENTITY_PACKAGES = "multicados.scanned-packages.entity";
	public static final String SCANNED_FILE_RESOURCE_PACKAGE = "multicados.scanned-packages.file";

	public static final String READ_FAILURE_EXCEPTION_THROWING_STRATEGY = "multicados.crud.security.read.failurestrategy";

	public static final String DUMMY_DATABASE_MODE = "multicados.dummy-database.initializer";
	public static final String DUMMY_DATABASE_PATH = "multicados.dummy-database.path";

	public static final String SECURITY_PUBLIC_END_POINTS = "multicados.security.endpoints.public";
	public static final String SECURITY_JWT_TOKEN_END_POINT = "multicados.security.jwt.token.endpoint";
	public static final String SECURITY_JWT_LOGOUT_END_POINT = "multicados.security.jwt.logout.endpoint";
	public static final String SECURITY_JWT_TOKEN_USERNAME = "multicados.security.jwt.param.username";
	public static final String SECURITY_JWT_TOKEN_PASSWORD = "multicados.security.jwt.param.password";
	public static final String SECURITY_JWT_HEADER_PREFIX = "multicados.security.jwt.headerprefix";
	public static final String SECURITY_JWT_COOKIE_NAME = "multicados.security.jwt.cookiename";
	public static final String SECURITY_JWT_SECRET = "multicados.security.jwt.secret";
	public static final String SECURITY_JWT_EXPIRATION_DURATION = "multicados.security.jwt.expiration";
	public static final String SECURITY_DEV_CLIENT_PORTS = "multicados.security.dev.client.ports";
	public static final String SECURITY_RSA_PRIVATE_KEY_PATH = "multicados.security.rsa.private";
	public static final String SECURITY_RSA_PUBLIC_KEY_PATH = "multicados.security.rsa.public";
	
	public static final String FILE_RESOURCE_IDENTIFIER_LENGTH = "multicados.file.id.length";
	public static final String FILE_RESOURCE_IDENTIFIER_DELIMITER = "multicados.file.id.delimiter";
	public static final String FILE_RESOURCE_IMAGE_STANDARD = "multicados.file.image.standard";
	public static final String FILE_RESOURCE_ROOT_DIRECTORY = "multicados.file.directory";
	public static final String FILE_RESOURCE_PUBLIC_DIRECTORY = "multicados.file.directory.public";

	public static final String DOMAIN_NAMED_RESOURCE_ACCEPTED_CHARS = "multicados.domain.resource.named.chars";
	public static final String DOMAIN_NAMED_RESOURCE_MAX_LENGTH = "multicados.domain.resource.named.max";
	public static final String DOMAIN_NAMED_RESOURCE_MIN_LENGTH = "multicados.domain.resource.named.min";
	public static final String DOMAIN_NAMED_RESOURCE_ACCEPTED_NATURAL_ALPHABET = "multicados.domain.resource.named.alphabet";
	public static final String DOMAIN_NAMED_RESOURCE_ACCEPTED_NATURAL_NUMERIC = "multicados.domain.resource.named.numeric";
	public static final String DOMAIN_NAMED_RESOURCE_ACCEPTED_LITERAL = "multicados.domain.resource.named.literal";
	
	public static final String CUSTOMER_CREDENTIAL_RESET_REQUEST_COOKIE_NAME = "multicados.customer.password.reset.request.cookie.name"; 
	public static final String CUSTOMER_CREDENTIAL_RESET_REQUEST_COOKIE_EXPIRATION = "multicados.customer.password.reset.request.cookie.expiration";
	public static final String CUSTOMER_CREDENTIAL_RESET_HOTP_UPPER_FACTOR = "multicados.customer.password.reset.hotp.factor.upper";
	public static final String CUSTOMER_CREDENTIAL_RESET_HOTP_LOWER_FACTOR = "multicados.customer.password.reset.hotp.factor.lower";
	public static final String CUSTOMER_CREDENTIAL_RESET_HOTP_LENGTH = "multicados.customer.password.reset.hotp.length";
	
	public static final String USER_DEFAULT_PHOTO_FILENAME = "multicados.user.photo";

	public static final String DEFAULT_PRODUCTION_PROFILE = "PROD";

	public static final String HBM_FLUSH_MODE = "org.hibernate.flushMode";

}
