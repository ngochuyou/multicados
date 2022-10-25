/**
 *
 */
package multicados.domain.entity.entities;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import multicados.domain.AbstractEntity;
import multicados.domain.entity.ApprovalInformations;
import multicados.domain.entity.AuditInformations;
import multicados.domain.entity.PermanentEntity;
import multicados.domain.entity.converter.StringListConverter;
import multicados.internal.domain.ApprovableResource;
import multicados.internal.domain.AuditableResource;
import multicados.internal.domain.EncryptedIdentifierResource;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.annotation.Name;
import multicados.internal.helper.Base32;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "products")
public class Product extends PermanentEntity<BigInteger>
		implements NamedResource, AuditableResource<String, Operator, LocalDateTime>,
		EncryptedIdentifierResource<BigInteger>, ApprovableResource<String, Head, LocalDateTime> {

	public static final int MAXIMUM_IMAGES_AMOUNT = 20;
	public static final int MAXIMUM_IMAGES_COLUMN_LENGTH = MAXIMUM_IMAGES_AMOUNT * 35;
	public static final int MAXIMUM_MATERIAL_LENGTH = 50;

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = SHARED_TABLE_GENERATOR)
	@TableGenerator(name = SHARED_TABLE_GENERATOR, initialValue = Base32.CROCKFORD_10A
			- 1, allocationSize = 1, table = SHARED_TABLE_GENERATOR_TABLENAME)
	@Column(updatable = false, columnDefinition = MYSQL_BIGINT_COLUMN_DEFINITION)
	private BigInteger id;

	@Column(nullable = false, unique = true)
	private String code;

	@Name
	@Column(nullable = false, unique = true)
	private String name;

	@Embedded
	private AuditInformations auditInformations;

	@Embedded
	private ApprovalInformations approvalInformations;

	@Column(length = MAXIMUM_MATERIAL_LENGTH)
	private String material;

	@Column(length = MAXIMUM_IMAGES_COLUMN_LENGTH)
	@Convert(converter = StringListConverter.class)
	private List<String> images;

	@Column(columnDefinition = "TEXT")
	private String description;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", referencedColumnName = AbstractEntity.ID_C)
	private Category category;

	@Column(columnDefinition = "TINYINT")
	private int rating;

	public Product() {
		auditInformations = new AuditInformations();
		approvalInformations = new ApprovalInformations();
	}

	@Override
	public BigInteger getId() {
		return id;
	}

	@Override
	public void setId(BigInteger id) {
		this.id = id;
	}

	@Override
	public void setCode(String encryptedCode) {
		code = encryptedCode;
	}

	public String getCode() {
		return code;
	}

	@Override
	public LocalDateTime getCreatedTimestamp() {
		return auditInformations.getCreatedTimestamp();
	}

	@Override
	public void setCreatedTimestamp(LocalDateTime timestamp) {
		auditInformations.setCreatedTimestamp(timestamp);
	}

	@Override
	public Operator getCreator() {
		return auditInformations.getCreator();
	}

	@Override
	public void setCreator(Operator creator) {
		auditInformations.setCreator(creator);
	}

	@Override
	public LocalDateTime getUpdatedTimestamp() {
		return auditInformations.getUpdatedTimestamp();
	}

	@Override
	public void setUpdatedTimestamp(LocalDateTime timestamp) {
		auditInformations.setUpdatedTimestamp(timestamp);
	}

	@Override
	public Operator getUpdater() {
		return auditInformations.getUpdater();
	}

	@Override
	public void setUpdater(Operator updater) {
		auditInformations.setUpdater(updater);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public AuditInformations getAuditInformations() {
		return auditInformations;
	}

	public void setAuditInformations(AuditInformations auditInformations) {
		this.auditInformations = auditInformations;
	}

	public String getMaterial() {
		return material;
	}

	public void setMaterial(String material) {
		this.material = material;
	}

	public List<String> getImages() {
		return images;
	}

	public void setImages(List<String> images) {
		this.images = images;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	@Override
	public Head getApprovedBy() {
		return approvalInformations.getApprovedBy();
	}

	@Override
	public void setApprovedBy(Head approvedBy) {
		approvalInformations.setApprovedBy(approvedBy);
	}

	@Override
	public LocalDateTime getApprovedTimestamp() {
		return approvalInformations.getApprovedTimestamp();
	}

	@Override
	public void setApprovedTimestamp(LocalDateTime approvedTimestamp) {
		approvalInformations.setApprovedTimestamp(approvedTimestamp);
	}

}
