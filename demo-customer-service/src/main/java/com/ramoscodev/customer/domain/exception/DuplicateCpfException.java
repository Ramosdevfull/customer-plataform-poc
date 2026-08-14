package com.ramoscodev.customer.domain.exception;

public class DuplicateCpfException extends RuntimeException {

	public DuplicateCpfException(String cpf) {
		super("CPF já cadastrado no sistema: " + cpf);
	}
}
