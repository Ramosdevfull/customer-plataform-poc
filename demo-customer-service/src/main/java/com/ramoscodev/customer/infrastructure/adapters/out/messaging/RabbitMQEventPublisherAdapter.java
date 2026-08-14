package com.ramoscodev.customer.infrastructure.adapters.out.messaging;

import com.ramoscodev.customer.domain.model.Customer;
import com.ramoscodev.customer.domain.port.out.EventPublisherPort;
import com.ramoscodev.customer.infrastructure.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RabbitMQEventPublisherAdapter implements EventPublisherPort {

	private final RabbitTemplate rabbitTemplate;

	public RabbitMQEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	public void publishCustomerCreated(Customer customer) {
		Map<String, Object> eventPayload = new HashMap<>();
		eventPayload.put("eventId", UUID.randomUUID().toString());
		eventPayload.put("eventType", "CUSTOMER_CREATED");
		eventPayload.put("customerId", customer.getId());
		eventPayload.put("name", customer.getName());
		eventPayload.put("cpf", customer.getCpf());
		eventPayload.put("email", customer.getEmail());
		eventPayload.put("status", customer.getStatus().name());
		eventPayload.put("createdAt", Instant.now().toString());

		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_CUSTOMER, RabbitMQConfig.RK_CREATED, eventPayload);
	}
}
