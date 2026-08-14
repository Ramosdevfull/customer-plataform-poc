package com.ramoscodev.customer.application.usecase;

import com.ramoscodev.customer.domain.exception.CustomerNotFoundException;
import com.ramoscodev.customer.domain.model.Customer;
import com.ramoscodev.customer.domain.model.CustomerStatus;
import com.ramoscodev.customer.domain.model.ScoreInfo;
import com.ramoscodev.customer.domain.port.out.CustomerRepositoryPort;
import com.ramoscodev.customer.domain.port.out.ScoreClientPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetCustomerScoreUseCaseTest {

	private CustomerRepositoryPort repositoryPort;
	private ScoreClientPort scoreClientPort;
	private GetCustomerScoreUseCase useCase;

	@BeforeEach
	void setUp() {
		repositoryPort = mock(CustomerRepositoryPort.class);
		scoreClientPort = mock(ScoreClientPort.class);
		useCase = new GetCustomerScoreUseCase(repositoryPort, scoreClientPort);
	}

	@Test
	@DisplayName("Deve retornar o score consultando pelo CPF do cliente")
	void shouldReturnScoreByCpf() {
		Customer customer = new Customer(1L, "João Silva", "12345678901", "joao@email.com", CustomerStatus.ACTIVE);
		when(repositoryPort.findById(1L)).thenReturn(Optional.of(customer));
		when(scoreClientPort.getScoreByCpf("12345678901"))
				.thenReturn(new ScoreInfo("12345678901", 750, "LOW_RISK"));

		ScoreInfo result = useCase.execute(1L);

		assertEquals(750, result.score());
		assertEquals("LOW_RISK", result.classification());
		verify(scoreClientPort).getScoreByCpf("12345678901");
	}

	@Test
	@DisplayName("Deve lançar exceção quando o cliente não existe")
	void shouldThrowWhenCustomerNotFound() {
		when(repositoryPort.findById(99L)).thenReturn(Optional.empty());

		assertThrows(CustomerNotFoundException.class, () -> useCase.execute(99L));
		verify(scoreClientPort, never()).getScoreByCpf(org.mockito.ArgumentMatchers.anyString());
	}
}
