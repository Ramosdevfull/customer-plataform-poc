package com.ramoscodev.customer.infrastructure.adapters.out.persistence;

import com.ramoscodev.customer.domain.model.CustomerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_customers")
public class CustomerEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true, length = 11)
	private String cpf;

	@Column(nullable = false, unique = true)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CustomerStatus status;

	public CustomerEntity() {
	}

	public CustomerEntity(Long id, String name, String cpf, String email, CustomerStatus status) {
		this.id = id;
		this.name = name;
		this.cpf = cpf;
		this.email = email;
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getCpf() {
		return cpf;
	}

	public String getEmail() {
		return email;
	}

	public CustomerStatus getStatus() {
		return status;
	}
}
