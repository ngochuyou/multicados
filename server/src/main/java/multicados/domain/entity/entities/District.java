/**
 * 
 */
package multicados.domain.entity.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import multicados.domain.entity.PermanentEntity;
import multicados.internal.domain.NamedResource;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "districts", indexes = @Index(columnList = District.$index))
public class District extends PermanentEntity<Integer> implements NamedResource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false)
	private String name;

	@ManyToOne(optional = false)
	@JoinColumn(name = District_.PROVINCE, referencedColumnName = Province_.ID)
	private Province province;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public Province getProvince() {
		return province;
	}

	public void setProvince(Province province) {
		this.province = province;
	}

	public static final String $index = "id, active";

}
