package com.ramoscodev.customer.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	public static final String QUEUE_STATUS_CHANGE = "customer.status.change.queue";
	public static final String QUEUE_CREATED = "customer.created.queue";
	public static final String EXCHANGE_CUSTOMER = "customer.exchange";
	public static final String EXCHANGE_DLX = "customer.dlx.exchange";
	public static final String QUEUE_STATUS_CHANGE_DLQ = "customer.status.change.dlq";

	public static final String RK_CREATED = "customer.created";
	public static final String RK_STATUS_CHANGE = "customer.status.change";
	public static final String RK_DLQ = "customer.status.change.dlq";

	@Bean
	public Queue statusChangeQueue() {
		return QueueBuilder.durable(QUEUE_STATUS_CHANGE)
				.withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
				.withArgument("x-dead-letter-routing-key", RK_DLQ)
				.build();
	}

	@Bean
	public Queue createdQueue() {
		return new Queue(QUEUE_CREATED, true);
	}

	@Bean
	public Queue statusChangeDlq() {
		return new Queue(QUEUE_STATUS_CHANGE_DLQ, true);
	}

	@Bean
	public TopicExchange customerExchange() {
		return new TopicExchange(EXCHANGE_CUSTOMER);
	}

	@Bean
	public TopicExchange customerDlxExchange() {
		return new TopicExchange(EXCHANGE_DLX);
	}

	@Bean
	public Binding createdBinding() {
		return BindingBuilder.bind(createdQueue()).to(customerExchange()).with(RK_CREATED);
	}

	@Bean
	public Binding statusChangeBinding() {
		return BindingBuilder.bind(statusChangeQueue()).to(customerExchange()).with(RK_STATUS_CHANGE);
	}

	@Bean
	public Binding dlqBinding() {
		return BindingBuilder.bind(statusChangeDlq()).to(customerDlxExchange()).with(RK_DLQ);
	}

	@Bean
	public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}
}
