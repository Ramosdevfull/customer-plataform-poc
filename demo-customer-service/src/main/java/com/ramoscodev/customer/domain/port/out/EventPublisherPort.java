package com.ramoscodev.customer.domain.port.out;

import com.ramoscodev.customer.domain.model.Customer;

public interface EventPublisherPort {

	void publishCustomerCreated(Customer customer);
}
