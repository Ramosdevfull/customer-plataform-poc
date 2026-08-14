package com.ramoscodev.customer.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomerRequestDTO(

		@NotBlank(message = "O nome é obrigatório")
		String name,

		@NotBlank(message = "O CPF é obrigatório")
		@Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos")
		String cpf,

		@NotBlank(message = "O e-mail é obrigatório")
		@Email(message = "Formato de e-mail inválido")
		String email
) {
}
