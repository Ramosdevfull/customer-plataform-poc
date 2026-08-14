package com.ramoscodev.customer.infrastructure.adapters.out.http;

import com.ramoscodev.customer.domain.model.ScoreInfo;
import com.ramoscodev.customer.domain.port.out.ScoreClientPort;
import org.springframework.stereotype.Component;

@Component
public class ScoreClientAdapter implements ScoreClientPort {

	private final ScoreHttpClient scoreHttpClient;

	public ScoreClientAdapter(ScoreHttpClient scoreHttpClient) {
		this.scoreHttpClient = scoreHttpClient;
	}

	@Override
	public ScoreInfo getScoreByCpf(String cpf) {
		return scoreHttpClient.call(cpf).join();
	}
}
