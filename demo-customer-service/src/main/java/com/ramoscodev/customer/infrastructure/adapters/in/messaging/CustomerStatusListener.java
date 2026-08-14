package com.ramoscodev.customer.infrastructure.adapters.in.messaging;

import com.ramoscodev.customer.application.usecase.ChangeCustomerStatusUseCase;
import com.ramoscodev.customer.infrastructure.adapters.out.persistence.ProcessedMessageEntity;
import com.ramoscodev.customer.infrastructure.adapters.out.persistence.SpringDataProcessedMessageRepository;
import com.ramoscodev.customer.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class CustomerStatusListener {

	private static final Logger log = LoggerFactory.getLogger(CustomerStatusListener.class);

	private final ChangeCustomerStatusUseCase changeCustomerStatusUseCase;
	private final SpringDataProcessedMessageRepository processedMessageRepository;

	public CustomerStatusListener(ChangeCustomerStatusUseCase changeCustomerStatusUseCase,
			SpringDataProcessedMessageRepository processedMessageRepository) {
		this.changeCustomerStatusUseCase = changeCustomerStatusUseCase;
		this.processedMessageRepository = processedMessageRepository;
	}

	@RabbitListener(queues = RabbitMQConfig.QUEUE_STATUS_CHANGE)
	@Transactional
	public void receiveStatusChange(CustomerStatusChangeEvent event) {
		if (event == null || event.eventId() == null || event.eventId().isBlank()) {
			throw new IllegalArgumentException("Evento inválido: eventId ausente.");
		}

		if (processedMessageRepository.existsById(event.eventId())) {
			log.info("Evento com ID {} já foi processado. Ignorando.", event.eventId());
			return;
		}

		changeCustomerStatusUseCase.execute(event.customerId(), event.status());

		processedMessageRepository.save(new ProcessedMessageEntity(event.eventId(), LocalDateTime.now()));
	}
}
