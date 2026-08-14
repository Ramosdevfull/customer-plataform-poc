package com.ramoscodev.customer.infrastructure.adapters.out.persistence;

import com.ramoscodev.customer.domain.model.Customer;
import com.ramoscodev.customer.domain.model.CustomerStatus;
import com.ramoscodev.customer.domain.port.out.CustomerRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CustomerRepositoryAdapter implements CustomerRepositoryPort {

	private final SpringDataCustomerRepository repository;

	public CustomerRepositoryAdapter(SpringDataCustomerRepository repository) {
		this.repository = repository;
	}

	@Override
	public Customer save(Customer customer) {
		CustomerEntity entity = new CustomerEntity(
				customer.getId(), customer.getName(), customer.getCpf(), customer.getEmail(), customer.getStatus());
		CustomerEntity saved = repository.save(entity);
		return toDomain(saved);
	}

	@Override
	public Optional<Customer> findById(Long id) {
		return repository.findById(id).map(this::toDomain);
	}

	@Override
	public Optional<Customer> findByCpf(String cpf) {
		return repository.findByCpf(cpf).map(this::toDomain);
	}

	@Override
	public List<Customer> findAll() {
		return repository.findAll().stream().map(this::toDomain).toList();
	}

	@Override
	public List<Customer> findByNameContaining(String name) {
		return repository.findByNameContainingIgnoreCase(name).stream().map(this::toDomain).toList();
	}

	@Override
	public List<Customer> findByStatus(CustomerStatus status) {
		return repository.findByStatus(status).stream().map(this::toDomain).toList();
	}

	@Override
	public void deleteById(Long id) {
		repository.deleteById(id);
	}

	@Override
	public boolean existsByCpf(String cpf) {
		return repository.existsByCpf(cpf);
	}

	private Customer toDomain(CustomerEntity entity) {
		return new Customer(entity.getId(), entity.getName(), entity.getCpf(), entity.getEmail(), entity.getStatus());
	}
}
