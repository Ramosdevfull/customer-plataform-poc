package com.ramoscodev.customer.application.usecase;

import com.ramoscodev.customer.domain.exception.DuplicateCpfException;
import com.ramoscodev.customer.domain.model.Customer;
import com.ramoscodev.customer.domain.model.CustomerStatus;
import com.ramoscodev.customer.domain.port.out.CustomerRepositoryPort;
import com.ramoscodev.customer.domain.port.out.EventPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateCustomerUseCaseTest {

	private CustomerRepositoryPort repositoryPort;
	private EventPublisherPort eventPublisherPort;
	private CreateCustomerUseCase useCase;

	@BeforeEach
	void setUp() {
		repositoryPort = mock(CustomerRepositoryPort.class);
		eventPublisherPort = mock(EventPublisherPort.class);
		useCase = new CreateCustomerUseCase(repositoryPort, eventPublisherPort);
	}

	@Test
	@DisplayName("Deve criar cliente com sucesso, definir status ACTIVE e publicar evento")
	void shouldCreateCustomerSuccessfully() {
		Customer customer = new Customer(null, "João Silva", "12345678901", "joao@email.com", null);

		when(repositoryPort.existsByCpf("12345678901")).thenReturn(false);
		when(repositoryPort.save(any(Customer.class))).thenAnswer(invocation -> {
			Customer c = invocation.getArgument(0);
			c.setId(1L);
			return c;
		});

		Customer result = useCase.execute(customer);

		assertNotNull(result.getId());
		assertEquals(CustomerStatus.ACTIVE, result.getStatus());
		verify(eventPublisherPort, times(1)).publishCustomerCreated(result);
	}

	@Test
	@DisplayName("Deve lançar exceção ao tentar cadastrar CPF duplicado")
	void shouldThrowExceptionWhenCpfExists() {
		Customer customer = new Customer(null, "João Silva", "12345678901", "joao@email.com", null);

		when(repositoryPort.existsByCpf("12345678901")).thenReturn(true);

		assertThrows(DuplicateCpfException.class, () -> useCase.execute(customer));
		verify(repositoryPort, never()).save(any());
		verify(eventPublisherPort, never()).publishCustomerCreated(any());
	}
}
