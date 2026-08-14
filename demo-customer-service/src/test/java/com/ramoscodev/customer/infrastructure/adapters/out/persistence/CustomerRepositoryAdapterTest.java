package com.ramoscodev.customer.infrastructure.adapters.out.persistence;

import com.ramoscodev.customer.domain.model.Customer;
import com.ramoscodev.customer.domain.model.CustomerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerRepositoryAdapterTest {

	private SpringDataCustomerRepository repository;
	private CustomerRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		repository = mock(SpringDataCustomerRepository.class);
		adapter = new CustomerRepositoryAdapter(repository);
	}

	@Test
	@DisplayName("Deve salvar e retornar o domínio")
	void shouldSaveAndMapToDomain() {
		Customer customer = new Customer(null, "João", "12345678901", "joao@email.com", CustomerStatus.ACTIVE);
		when(repository.save(org.mockito.ArgumentMatchers.any(CustomerEntity.class)))
				.thenReturn(new CustomerEntity(1L, "João", "12345678901", "joao@email.com", CustomerStatus.ACTIVE));

		Customer saved = adapter.save(customer);

		assertEquals(1L, saved.getId());
		assertEquals("João", saved.getName());
	}

	@Test
	@DisplayName("Deve buscar por id e mapear para domínio")
	void shouldFindByIdAndMap() {
		when(repository.findById(1L))
				.thenReturn(Optional.of(new CustomerEntity(1L, "Maria", "98765432100", "maria@email.com", CustomerStatus.INACTIVE)));

		Optional<Customer> found = adapter.findById(1L);

		assertTrue(found.isPresent());
		assertEquals(CustomerStatus.INACTIVE, found.get().getStatus());
	}

	@Test
	@DisplayName("Deve delegar a verificação de CPF existente")
	void shouldDelegateExistsByCpf() {
		when(repository.existsByCpf("12345678901")).thenReturn(true);

		assertTrue(adapter.existsByCpf("12345678901"));
	}

	@Test
	@DisplayName("Deve buscar por nome e status")
	void shouldSearchByNameAndStatus() {
		CustomerEntity entity = new CustomerEntity(1L, "Ana", "11111111111", "ana@email.com", CustomerStatus.ACTIVE);
		when(repository.findByNameContainingIgnoreCase("an")).thenReturn(List.of(entity));
		when(repository.findByStatus(CustomerStatus.ACTIVE)).thenReturn(List.of(entity));

		assertEquals(1, adapter.findByNameContaining("an").size());
		assertEquals(1, adapter.findByStatus(CustomerStatus.ACTIVE).size());
		verify(repository).findByStatus(CustomerStatus.ACTIVE);
	}
}
