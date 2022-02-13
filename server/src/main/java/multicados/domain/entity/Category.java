/**
 * 
 */
package multicados.domain.entity;

import static multicados.application.Common.SHARED_TABLE_GENERATOR;

import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import multicados.application.Common;
import multicados.internal.domain.EncryptedIdentifierResource;
import multicados.internal.domain.NamedResource;
import multicados.internal.helper.Base32;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "categories")
public class Category extends PermanentEntity<Long> implements NamedResource, EncryptedIdentifierResource<Long> {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = SHARED_TABLE_GENERATOR)
	// we start from 1033 so that the generator takes 1034
	@TableGenerator(name = SHARED_TABLE_GENERATOR, initialValue = Base32.CROCKFORD_10A
			- 1, allocationSize = 1, table = Common.SHARED_TABLE_GENERATOR_TABLENAME)
	@Column(updatable = false)
	private Long id;

	@Column(unique = true, length = MAXIMUM_CODE_LENGTH)
	private String code;

	@Column(nullable = false, unique = true)
	private String name;

	@Column
	private String description;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	/* ==========METADATAS========== */
	public static final String code_ = "code";

	public static final String description_ = "description";

	public static final String products_ = "products";

	public static final int MAXIMUM_CODE_LENGTH = 5;
	public static final int MAX_DESCRIPTION_LENGTH = 255;
	// @formatter:off
	public static final Pattern DESCRIPTION_PATTERN = Pattern.compile(
			String.format("^[%s\\p{L}\\p{N}\s\\.,()\\[\\]_\\-+=/\\\\!@#$%%^&*'\"?]{0,%d}$",
					StringHelper.VIETNAMESE_CHARACTERS,
					MAX_DESCRIPTION_LENGTH));
	// @formatter:on
	/* ==========METADATAS========== */
}