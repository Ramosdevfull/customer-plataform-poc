package com.ramoscodev.customer.infrastructure.adapters.out.http;

import com.ramoscodev.customer.domain.model.ScoreInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class ScoreHttpClient {

	private final RestTemplate restTemplate;
	private final String scoreServiceUrl;

	public ScoreHttpClient(RestTemplate restTemplate,
			@Value("${external.score-service.url}") String scoreServiceUrl) {
		this.restTemplate = restTemplate;
		this.scoreServiceUrl = scoreServiceUrl;
	}

	@CircuitBreaker(name = "scoreService", fallbackMethod = "fallbackScore")
	@TimeLimiter(name = "scoreService", fallbackMethod = "fallbackScore")
	@Retry(name = "scoreService", fallbackMethod = "fallbackScore")
	public CompletableFuture<ScoreInfo> call(String cpf) {
		return CompletableFuture.supplyAsync(() -> doRequest(cpf));
	}

	ScoreInfo doRequest(String cpf) {
		String url = scoreServiceUrl + "/scores/" + cpf;
		Map<String, Object> response = restTemplate.getForObject(url, Map.class);

		Integer score = ((Number) response.get("score")).intValue();
		return new ScoreInfo((String) response.get("cpf"), score, (String) response.get("classification"));
	}

	public CompletableFuture<ScoreInfo> fallbackScore(String cpf, Throwable throwable) {
		return CompletableFuture.completedFuture(new ScoreInfo(cpf, 0, "UNAVAILABLE"));
	}
}
