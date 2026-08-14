package com.ramoscodev.customer.infrastructure.adapters.out.http;

import com.ramoscodev.customer.domain.model.ScoreInfo;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ScoreClientAdapterTest {

	private static final String BASE_URL = "http://score-service";

	private RestTemplate restTemplate;
	private ScoreHttpClient client;

	@BeforeEach
	void setUp() {
		restTemplate = new RestTemplate();
		client = new ScoreHttpClient(restTemplate, BASE_URL);
	}

	@Test
	@DisplayName("Deve retornar o score quando o serviço externo responde com sucesso")
	void shouldReturnScoreOnSuccess() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(requestTo(BASE_URL + "/scores/12345678901"))
				.andRespond(withSuccess("""
						{"cpf":"12345678901","score":750,"classification":"LOW_RISK"}
						""", MediaType.APPLICATION_JSON));

		ScoreInfo info = client.call("12345678901").join();

		assertEquals(750, info.score());
		assertEquals("LOW_RISK", info.classification());
		server.verify();
	}

	@Test
	@DisplayName("O fallback deve retornar score zero com classificação UNAVAILABLE")
	void fallbackShouldReturnUnavailable() {
		ScoreInfo info = client.fallbackScore("12345678901", new RuntimeException("boom")).join();

		assertEquals(0, info.score());
		assertEquals("UNAVAILABLE", info.classification());
	}

	@Test
	@DisplayName("TimeLimiter deve estourar o tempo limite (2s) em resposta lenta e o fallback é utilizado")
	void timeLimiterShouldTripOnSlowResponse() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(requestTo(BASE_URL + "/scores/12345678901"))
				.andRespond(request -> {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return withSuccess("""
							{"cpf":"12345678901","score":800,"classification":"LOW_RISK"}
							""", MediaType.APPLICATION_JSON).createResponse(request);
				});

		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		try {
			TimeLimiter timeLimiter = TimeLimiter.of(Duration.ofSeconds(2));
			Supplier<CompletionStage<ScoreInfo>> decorated =
					timeLimiter.decorateCompletionStage(scheduler, () -> client.call("12345678901"));

			long start = System.currentTimeMillis();
			CompletionStage<ScoreInfo> stage = decorated.get();
			CompletionException ex = assertThrows(CompletionException.class,
					() -> stage.toCompletableFuture().join());
			long elapsed = System.currentTimeMillis() - start;

			assertTrue(elapsed < 3000, "deve estourar o tempo limite em ~2s");
			assertInstanceOf(TimeoutException.class, ex.getCause());

			ScoreInfo fallback = client.fallbackScore("12345678901", new TimeoutException()).join();
			assertEquals("UNAVAILABLE", fallback.classification());
		} finally {
			scheduler.shutdownNow();
		}
	}

	@Test
	@DisplayName("Circuit Breaker deve abrir após falhas consecutivas e o fallback é utilizado")
	void circuitBreakerShouldOpenAfterFailures() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(requestTo(BASE_URL + "/scores/12345678901"))
				.andRespond(withServerError());

		CircuitBreaker circuitBreaker = CircuitBreaker.of("scoreService", CircuitBreakerConfig.custom()
				.slidingWindowSize(5)
				.failureRateThreshold(50)
				.waitDurationInOpenState(Duration.ofSeconds(1))
				.build());

		Supplier<CompletionStage<ScoreInfo>> decorated =
				circuitBreaker.decorateCompletionStage(() -> client.call("12345678901"));

		for (int i = 0; i < 5; i++) {
			assertThrows(CompletionException.class, () -> decorated.get().toCompletableFuture().join());
		}

		CompletionException ex = assertThrows(CompletionException.class,
				() -> decorated.get().toCompletableFuture().join());
		assertInstanceOf(CallNotPermittedException.class, ex.getCause());

		ScoreInfo fallback = client.fallbackScore("12345678901", new RuntimeException("circuit open")).join();
		assertEquals("UNAVAILABLE", fallback.classification());
	}
}
