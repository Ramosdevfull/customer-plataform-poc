package com.ramoscodev.customer.infrastructure.adapters.in.messaging;

import com.ramoscodev.customer.domain.model.CustomerStatus;

public record CustomerStatusChangeEvent(
		String eventId,
		String eventType,
		Long customerId,
		CustomerStatus status
) {
}
