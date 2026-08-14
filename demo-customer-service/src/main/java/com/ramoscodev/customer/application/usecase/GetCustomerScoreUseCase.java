package com.ramoscodev.customer.application.usecase;

import com.ramoscodev.customer.domain.exception.CustomerNotFoundException;
import com.ramoscodev.customer.domain.model.Customer;
import com.ramoscodev.customer.domain.model.ScoreInfo;
import com.ramoscodev.customer.domain.port.out.CustomerRepositoryPort;
import com.ramoscodev.customer.domain.port.out.ScoreClientPort;
import org.springframework.stereotype.Service;

@Service
public class GetCustomerScoreUseCase {

	private final CustomerRepositoryPort customerRepositoryPort;
	private final ScoreClientPort scoreClientPort;

	public GetCustomerScoreUseCase(CustomerRepositoryPort customerRepositoryPort, ScoreClientPort scoreClientPort) {
		this.customerRepositoryPort = customerRepositoryPort;
		this.scoreClientPort = scoreClientPort;
	}

	public ScoreInfo execute(Long customerId) {
		Customer customer = customerRepositoryPort.findById(customerId)
				.orElseThrow(() -> new CustomerNotFoundException(customerId));

		return scoreClientPort.getScoreByCpf(customer.getCpf());
	}
}
