package com.ramoscodev.customer.infrastructure.adapters.in.messaging;

import com.ramoscodev.customer.application.usecase.ChangeCustomerStatusUseCase;
import com.ramoscodev.customer.domain.model.CustomerStatus;
import com.ramoscodev.customer.infrastructure.adapters.out.persistence.ProcessedMessageEntity;
import com.ramoscodev.customer.infrastructure.adapters.out.persistence.SpringDataProcessedMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerStatusListenerTest {

	private ChangeCustomerStatusUseCase changeCustomerStatusUseCase;
	private SpringDataProcessedMessageRepository processedMessageRepository;
	private CustomerStatusListener listener;

	@BeforeEach
	void setUp() {
		changeCustomerStatusUseCase = mock(ChangeCustomerStatusUseCase.class);
		processedMessageRepository = mock(SpringDataProcessedMessageRepository.class);
		listener = new CustomerStatusListener(changeCustomerStatusUseCase, processedMessageRepository);
	}

	@Test
	@DisplayName("Deve processar evento e registrar idempotência")
	void shouldProcessEventAndRegisterIdempotency() {
		CustomerStatusChangeEvent event =
				new CustomerStatusChangeEvent("evt-001", "CUSTOMER_STATUS_CHANGE", 1L, CustomerStatus.INACTIVE);
		when(processedMessageRepository.existsById("evt-001")).thenReturn(false);

		listener.receiveStatusChange(event);

		verify(changeCustomerStatusUseCase).execute(1L, CustomerStatus.INACTIVE);
		verify(processedMessageRepository).save(any(ProcessedMessageEntity.class));
	}

	@Test
	@DisplayName("Deve ignorar evento duplicado sem reprocessar")
	void shouldIgnoreDuplicateEvent() {
		CustomerStatusChangeEvent event =
				new CustomerStatusChangeEvent("evt-001", "CUSTOMER_STATUS_CHANGE", 1L, CustomerStatus.INACTIVE);
		when(processedMessageRepository.existsById("evt-001")).thenReturn(true);

		listener.receiveStatusChange(event);

		verify(changeCustomerStatusUseCase, never()).execute(any(), any());
		verify(processedMessageRepository, never()).save(any(ProcessedMessageEntity.class));
	}

	@Test
	@DisplayName("Deve processar a mensagem apenas uma vez mesmo quando o evento duplicado chega em sequência")
	void shouldProcessMessageOnlyOnceWhenDuplicateEventReceived() {
		CustomerStatusChangeEvent event =
				new CustomerStatusChangeEvent("evt-unique-999", "CUSTOMER_STATUS_CHANGE", 1L, CustomerStatus.INACTIVE);
		when(processedMessageRepository.existsById("evt-unique-999")).thenReturn(false, true);

		listener.receiveStatusChange(event);
		listener.receiveStatusChange(event);

		verify(changeCustomerStatusUseCase, times(1)).execute(1L, CustomerStatus.INACTIVE);
		verify(processedMessageRepository, times(1)).save(any(ProcessedMessageEntity.class));
	}
}
