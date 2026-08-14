package com.ramoscodev.customer.infrastructure.config;

import com.ramoscodev.customer.domain.exception.CustomerNotFoundException;
import com.ramoscodev.customer.domain.exception.DuplicateCpfException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomerNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleCustomerNotFound(CustomerNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(DuplicateCpfException.class)
	public ResponseEntity<Map<String, Object>> handleDuplicateCpf(DuplicateCpfException ex) {
		return build(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return build(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + ex.getName());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, Object> errors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("timestamp", LocalDateTime.now());
		response.put("status", HttpStatus.BAD_REQUEST.value());
		response.put("errors", errors);
		return ResponseEntity.badRequest().body(response);
	}

	private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("timestamp", LocalDateTime.now());
		response.put("status", status.value());
		response.put("error", message);
		return ResponseEntity.status(status).body(response);
	}
}
