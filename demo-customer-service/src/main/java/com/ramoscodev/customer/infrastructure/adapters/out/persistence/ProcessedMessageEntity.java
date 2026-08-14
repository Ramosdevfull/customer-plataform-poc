package com.ramoscodev.customer.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_processed_messages")
public class ProcessedMessageEntity {

	@Id
	@Column(name = "event_id", nullable = false, length = 100)
	private String eventId;

	@Column(name = "processed_at", nullable = false)
	private LocalDateTime processedAt;

	public ProcessedMessageEntity() {
	}

	public ProcessedMessageEntity(String eventId, LocalDateTime processedAt) {
		this.eventId = eventId;
		this.processedAt = processedAt;
	}

	public String getEventId() {
		return eventId;
	}

	public LocalDateTime getProcessedAt() {
		return processedAt;
	}
}
