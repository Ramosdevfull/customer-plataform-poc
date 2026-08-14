package com.ramoscodev.customer.integration;

import com.ramoscodev.customer.domain.model.CustomerStatus;
import com.ramoscodev.customer.infrastructure.adapters.out.persistence.SpringDataCustomerRepository;
import com.ramoscodev.customer.infrastructure.adapters.out.persistence.SpringDataProcessedMessageRepository;
import com.ramoscodev.customer.infrastructure.config.RabbitMQConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("integration")
class CustomerServiceIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("customer_db")
			.withUsername("user")
			.withPassword("password");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private SpringDataCustomerRepository customerRepository;

	@Autowired
	private SpringDataProcessedMessageRepository processedMessageRepository;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@BeforeEach
	void resetCircuitBreaker() {
		circuitBreakerRegistry.circuitBreaker("scoreService").reset();
	}

	@Test
	@DisplayName("RF01/RF02 - CRUD completo e filtros de cliente")
	void shouldExecuteCompleteCustomerCrud() throws Exception {
		long id = createCustomer("João da Silva", "12345678901", "joao@email.com");

		mockMvc.perform(get("/customers/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.name").value("João da Silva"))
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		mockMvc.perform(get("/customers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].cpf").value("12345678901"));

		mockMvc.perform(get("/customers").param("status", "ACTIVE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(get("/customers/search").param("name", "João"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("João da Silva"));

		mockMvc.perform(put("/customers/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"João Atualizado","cpf":"12345678901","email":"joao.novo@email.com"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("João Atualizado"))
				.andExpect(jsonPath("$.email").value("joao.novo@email.com"));

		mockMvc.perform(delete("/customers/{id}", id))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/customers/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("RF01 - Deve rejeitar CPF duplicado com 409 e validação com 400")
	void shouldRejectDuplicateCpfAndValidationErrors() throws Exception {
		createCustomer("Maria Souza", "98765432100", "maria@email.com");

		mockMvc.perform(post("/customers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Maria Duplicada","cpf":"98765432100","email":"maria.dup@email.com"}
								"""))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/customers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"","cpf":"123","email":"email-invalido"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.cpf").exists())
				.andExpect(jsonPath("$.errors.email").exists());
	}

	@Test
	@DisplayName("RF03 - Deve retornar o score consultando o serviço externo")
	void shouldReturnScoreFromExternalService() throws Exception {
		long id = createCustomer("Cliente Score", "11122233344", "score@email.com");

		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/scores/11122233344")))
				.andRespond(withSuccess("""
						{"cpf":"11122233344","score":750,"classification":"LOW_RISK"}
						""", MediaType.APPLICATION_JSON));

		mockMvc.perform(get("/customers/{id}/score", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.score").value(750))
				.andExpect(jsonPath("$.classification").value("LOW_RISK"));
	}

	@Test
	@DisplayName("RNF04 - Fallback UNAVAILABLE quando o serviço de score está fora (500)")
	void shouldFallbackWhenScoreServiceIsDown() throws Exception {
		long id = createCustomer("Cliente Fora", "99988877766", "fora@email.com");

		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(ExpectedCount.manyTimes(), requestTo(org.hamcrest.Matchers.containsString("/scores/99988877766")))
				.andRespond(withServerError());

		for (int i = 0; i < 6; i++) {
			mockMvc.perform(get("/customers/{id}/score", id))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.score").value(0))
					.andExpect(jsonPath("$.classification").value("UNAVAILABLE"));
		}
	}

	@Test
	@DisplayName("RNF04 - TimeLimiter corta resposta lenta e retorna fallback UNAVAILABLE")
	void shouldFallbackWhenScoreServiceIsSlow() throws Exception {
		long id = createCustomer("Cliente Lento", "55544433322", "lento@email.com");

		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(ExpectedCount.manyTimes(), requestTo(org.hamcrest.Matchers.containsString("/scores/55544433322")))
				.andRespond(request -> {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return withSuccess("""
							{"cpf":"55544433322","score":800,"classification":"LOW_RISK"}
							""", MediaType.APPLICATION_JSON).createResponse(request);
				});

		long start = System.currentTimeMillis();
		mockMvc.perform(get("/customers/{id}/score", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.classification").value("UNAVAILABLE"));
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed < 4500, "deve responder via fallback bem antes da resposta lenta do mock (3s)");
	}

	@Test
	@DisplayName("RNF03/RF05 - Processa evento CUSTOMER_STATUS_CHANGE e é idempotente")
	void shouldProcessStatusChangeEventAndIgnoreDuplicates() throws Exception {
		long id = createCustomer("Pedro Eventos", "77766655544", "pedro.eventos@email.com");

		Map<String, Object> event = Map.of(
				"eventId", "evt-" + UUID.randomUUID(),
				"eventType", "CUSTOMER_STATUS_CHANGE",
				"customerId", id,
				"status", "INACTIVE");

		rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_STATUS_CHANGE, event);

		await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
				assertEquals(CustomerStatus.INACTIVE,
						customerRepository.findById(id).orElseThrow().getStatus()));

		assertEquals(1, processedMessageRepository.count());

		rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_STATUS_CHANGE, event);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
				assertEquals(1, processedMessageRepository.count()));

		Thread.sleep(1000);
		assertEquals(1, processedMessageRepository.count());
		assertEquals(CustomerStatus.INACTIVE, customerRepository.findById(id).orElseThrow().getStatus());
	}

	private long createCustomer(String name, String cpf, String email) throws Exception {
		String response = mockMvc.perform(post("/customers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"%s","cpf":"%s","email":"%s"}
								""".formatted(name, cpf, email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andReturn().getResponse().getContentAsString();

		JsonNode node = objectMapper.readTree(response);
		return node.get("id").asLong();
	}
}
