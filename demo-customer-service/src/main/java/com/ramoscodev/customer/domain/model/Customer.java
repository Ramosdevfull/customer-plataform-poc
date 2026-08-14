package com.ramoscodev.customer.domain.model;

public class Customer {

	private Long id;
	private String name;
	private String cpf;
	private String email;
	private CustomerStatus status;

	public Customer() {
	}

	public Customer(Long id, String name, String cpf, String email, CustomerStatus status) {
		this.id = id;
		this.name = name;
		this.cpf = cpf;
		this.email = email;
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public CustomerStatus getStatus() {
		return status;
	}

	public void setStatus(CustomerStatus status) {
		this.status = status;
	}
}
