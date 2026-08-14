package com.ramoscodev.customer.application.usecase;

import com.ramoscodev.customer.domain.exception.DuplicateCpfException;
import com.ramoscodev.customer.domain.model.Customer;
import com.ramoscodev.customer.domain.model.CustomerStatus;
import com.ramoscodev.customer.domain.port.out.CustomerRepositoryPort;
import com.ramoscodev.customer.domain.port.out.EventPublisherPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCustomerUseCase {

	private final CustomerRepositoryPort customerRepositoryPort;
	private final EventPublisherPort eventPublisherPort;

	public CreateCustomerUseCase(CustomerRepositoryPort customerRepositoryPort, EventPublisherPort eventPublisherPort) {
		this.customerRepositoryPort = customerRepositoryPort;
		this.eventPublisherPort = eventPublisherPort;
	}

	@Transactional
	public Customer execute(Customer customer) {
		if (customerRepositoryPort.existsByCpf(customer.getCpf())) {
			throw new DuplicateCpfException(customer.getCpf());
		}

		customer.setStatus(CustomerStatus.ACTIVE);
		Customer savedCustomer = customerRepositoryPort.save(customer);

		eventPublisherPort.publishCustomerCreated(savedCustomer);

		return savedCustomer;
	}
}
