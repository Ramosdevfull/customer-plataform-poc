package com.ramoscodev.customer.application.usecase;

import com.ramoscodev.customer.domain.exception.CustomerNotFoundException;
import com.ramoscodev.customer.domain.model.Customer;
import com.ramoscodev.customer.domain.model.CustomerStatus;
import com.ramoscodev.customer.domain.port.out.CustomerRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChangeCustomerStatusUseCaseTest {

	private CustomerRepositoryPort repositoryPort;
	private ChangeCustomerStatusUseCase useCase;

	@BeforeEach
	void setUp() {
		repositoryPort = mock(CustomerRepositoryPort.class);
		useCase = new ChangeCustomerStatusUseCase(repositoryPort);
	}

	@Test
	@DisplayName("Deve alterar o status do cliente")
	void shouldChangeCustomerStatus() {
		Customer customer = new Customer(1L, "Maria Souza", "98765432100", "maria@email.com", CustomerStatus.ACTIVE);
		when(repositoryPort.findById(1L)).thenReturn(Optional.of(customer));

		useCase.execute(1L, CustomerStatus.INACTIVE);

		assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
		verify(repositoryPort).save(customer);
	}

	@Test
	@DisplayName("Deve lançar exceção quando o cliente não existe")
	void shouldThrowWhenCustomerNotFound() {
		when(repositoryPort.findById(99L)).thenReturn(Optional.empty());

		assertThrows(CustomerNotFoundException.class, () -> useCase.execute(99L, CustomerStatus.INACTIVE));
		verify(repositoryPort, never()).save(any());
	}
}
