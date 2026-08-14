package com.ramoscodev.customer.domain.exception;

public class CustomerNotFoundException extends RuntimeException {

	public CustomerNotFoundException(Long id) {
		super("Cliente não encontrado com o ID: " + id);
	}
}
