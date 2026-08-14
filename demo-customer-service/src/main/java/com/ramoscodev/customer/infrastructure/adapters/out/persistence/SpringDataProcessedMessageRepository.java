package com.ramoscodev.customer.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProcessedMessageRepository extends JpaRepository<ProcessedMessageEntity, String> {
}
