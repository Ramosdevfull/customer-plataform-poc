package com.ramoscodev.customer.application.usecase;

import com.ramoscodev.customer.domain.exception.CustomerNotFoundException;
import com.ramoscodev.customer.domain.model.Customer;
import com.ramoscodev.customer.domain.model.CustomerStatus;
import com.ramoscodev.customer.domain.port.out.CustomerRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeCustomerStatusUseCase {

	private final CustomerRepositoryPort customerRepositoryPort;

	public ChangeCustomerStatusUseCase(CustomerRepositoryPort customerRepositoryPort) {
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Transactional
	public void execute(Long customerId, CustomerStatus newStatus) {
		Customer customer = customerRepositoryPort.findById(customerId)
				.orElseThrow(() -> new CustomerNotFoundException(customerId));

		customer.setStatus(newStatus);
		customerRepositoryPort.save(customer);
	}
}
