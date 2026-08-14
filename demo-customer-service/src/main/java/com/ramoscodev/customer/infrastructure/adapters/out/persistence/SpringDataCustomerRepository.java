package com.ramoscodev.customer.infrastructure.adapters.out.persistence;

import com.ramoscodev.customer.domain.model.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataCustomerRepository extends JpaRepository<CustomerEntity, Long> {

	Optional<CustomerEntity> findByCpf(String cpf);

	List<CustomerEntity> findByNameContainingIgnoreCase(String name);

	List<CustomerEntity> findByStatus(CustomerStatus status);

	boolean existsByCpf(String cpf);
}
