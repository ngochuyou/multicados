/**
 *
 */
package multicados.domain.entity.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

import multicados.domain.entity.Gender;
import multicados.domain.entity.PermanentEntity;
import multicados.domain.entity.Role;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User extends PermanentEntity<String> {

	@Id
	private String id;

	private String email;

	private String address;

	@Column(nullable = false)
	private String phone;

	@Column(nullable = false)
	private String lastName;

	@Column(nullable = false)
	private String firstName;

	@Enumerated(EnumType.STRING)
	@Column(length = 20, nullable = false)
	private Gender gender;

	private LocalDate birthDate;

	@Enumerated(EnumType.STRING)
	@Column(length = 20, nullable = false)
	private Role role;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private LocalDateTime credentialVersion;

	@Column(nullable = false)
	private Boolean locked;

	@Column(nullable = false, length = 40)
	private String photo;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public LocalDateTime getCredentialVersion() {
		return credentialVersion;
	}

	public void setCredentialVersion(LocalDateTime credentialVersion) {
		this.credentialVersion = credentialVersion;
	}

	@JsonProperty()
	public Boolean isLocked() {
		return locked;
	}

	public void setLocked(Boolean locked) {
		this.locked = locked;
	}

	public Boolean getLocked() {
		return locked;
	}

	public String getPhoto() {
		return photo;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}

}
