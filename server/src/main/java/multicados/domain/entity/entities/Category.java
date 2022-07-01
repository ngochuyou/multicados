/**
 *
 */
package multicados.domain.entity.entities;

import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import multicados.domain.AbstractEntity;
import multicados.domain.entity.PermanentEntity;
import multicados.domain.validator.CategoryValidator;
import multicados.internal.domain.EncryptedIdentifierResource;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.annotation.Name;
import multicados.internal.helper.Base32;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "categories")
public class Category extends PermanentEntity<BigInteger>
		implements NamedResource, EncryptedIdentifierResource<BigInteger> {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = AbstractEntity.SHARED_TABLE_GENERATOR)
	@TableGenerator(name = AbstractEntity.SHARED_TABLE_GENERATOR, initialValue = Base32.CROCKFORD_10A
			- 1, allocationSize = 1, table = AbstractEntity.SHARED_TABLE_GENERATOR_TABLENAME)
	@Column(updatable = false, columnDefinition = "BIGINT")
	private BigInteger id;

	@Column(unique = true, length = CategoryValidator.MAX_CODE_LENGTH)
	private String code;

	@Name
	@Column(unique = true, nullable = false)
	private String name;

	@Column
	private String description;

	@Override
	public BigInteger getId() {
		return id;
	}

	@Override
	public void setId(BigInteger id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	@Override
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

}