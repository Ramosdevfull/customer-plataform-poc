package com.ramoscodev.customer.infrastructure.adapters.in.web;

import com.ramoscodev.customer.application.usecase.CreateCustomerUseCase;
import com.ramoscodev.customer.application.usecase.GetCustomerScoreUseCase;
import com.ramoscodev.customer.domain.exception.CustomerNotFoundException;
import com.ramoscodev.customer.domain.model.Customer;
import com.ramoscodev.customer.domain.model.CustomerStatus;
import com.ramoscodev.customer.domain.model.ScoreInfo;
import com.ramoscodev.customer.domain.port.out.CustomerRepositoryPort;
import com.ramoscodev.customer.infrastructure.adapters.in.web.dto.CustomerRequestDTO;
import com.ramoscodev.customer.infrastructure.adapters.in.web.dto.CustomerResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

	private final CreateCustomerUseCase createCustomerUseCase;
	private final GetCustomerScoreUseCase getCustomerScoreUseCase;
	private final CustomerRepositoryPort customerRepositoryPort;

	public CustomerController(CreateCustomerUseCase createCustomerUseCase,
			GetCustomerScoreUseCase getCustomerScoreUseCase,
			CustomerRepositoryPort customerRepositoryPort) {
		this.createCustomerUseCase = createCustomerUseCase;
		this.getCustomerScoreUseCase = getCustomerScoreUseCase;
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@PostMapping
	public ResponseEntity<CustomerResponseDTO> create(@Valid @RequestBody CustomerRequestDTO dto) {
		Customer customer = new Customer(null, dto.name(), dto.cpf(), dto.email(), null);
		Customer saved = createCustomerUseCase.execute(customer);
		return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(saved));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CustomerResponseDTO> update(@PathVariable Long id, @Valid @RequestBody CustomerRequestDTO dto) {
		Customer existing = customerRepositoryPort.findById(id)
				.orElseThrow(() -> new CustomerNotFoundException(id));

		existing.setName(dto.name());
		existing.setCpf(dto.cpf());
		existing.setEmail(dto.email());

		Customer updated = customerRepositoryPort.save(existing);
		return ResponseEntity.ok(toResponseDTO(updated));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		if (customerRepositoryPort.findById(id).isEmpty()) {
			throw new CustomerNotFoundException(id);
		}
		customerRepositoryPort.deleteById(id);
	}

	@GetMapping("/{id}")
	public ResponseEntity<CustomerResponseDTO> getById(@PathVariable Long id) {
		return customerRepositoryPort.findById(id)
				.map(this::toResponseDTO)
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new CustomerNotFoundException(id));
	}

	@GetMapping
	public ResponseEntity<List<CustomerResponseDTO>> getAllOrByStatus(
			@RequestParam(required = false) CustomerStatus status) {
		List<Customer> customers = (status != null)
				? customerRepositoryPort.findByStatus(status)
				: customerRepositoryPort.findAll();

		return ResponseEntity.ok(customers.stream().map(this::toResponseDTO).toList());
	}

	@GetMapping("/search")
	public ResponseEntity<List<CustomerResponseDTO>> searchByName(@RequestParam String name) {
		List<Customer> customers = customerRepositoryPort.findByNameContaining(name);
		return ResponseEntity.ok(customers.stream().map(this::toResponseDTO).toList());
	}

	@GetMapping("/{id}/score")
	public ResponseEntity<ScoreInfo> getScore(@PathVariable Long id) {
		ScoreInfo scoreInfo = getCustomerScoreUseCase.execute(id);
		return ResponseEntity.ok(scoreInfo);
	}

	private CustomerResponseDTO toResponseDTO(Customer c) {
		return new CustomerResponseDTO(c.getId(), c.getName(), c.getCpf(), c.getEmail(), c.getStatus());
	}
}
