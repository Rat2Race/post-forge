package TEST.backend.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Account implements Serializable {

	@Id
	@GeneratedValue
	private Long id;
	private String username;
	private String password;
	private String roles;
	private int age;

}
