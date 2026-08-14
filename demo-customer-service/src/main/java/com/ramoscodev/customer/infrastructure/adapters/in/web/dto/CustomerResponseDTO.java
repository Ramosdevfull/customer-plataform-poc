package com.ramoscodev.customer.infrastructure.adapters.in.web.dto;

import com.ramoscodev.customer.domain.model.CustomerStatus;

public record CustomerResponseDTO(
		Long id,
		String name,
		String cpf,
		String email,
		CustomerStatus status
) {
}
