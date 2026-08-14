package com.ramoscodev.customer.domain.port.out;

import com.ramoscodev.customer.domain.model.Customer;
import com.ramoscodev.customer.domain.model.CustomerStatus;

import java.util.List;
import java.util.Optional;

public interface CustomerRepositoryPort {

	Customer save(Customer customer);

	Optional<Customer> findById(Long id);

	Optional<Customer> findByCpf(String cpf);

	List<Customer> findAll();

	List<Customer> findByNameContaining(String name);

	List<Customer> findByStatus(CustomerStatus status);

	void deleteById(Long id);

	boolean existsByCpf(String cpf);
}
