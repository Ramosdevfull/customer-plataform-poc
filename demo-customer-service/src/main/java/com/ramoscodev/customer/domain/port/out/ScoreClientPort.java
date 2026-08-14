package com.ramoscodev.customer.domain.port.out;

import com.ramoscodev.customer.domain.model.ScoreInfo;

public interface ScoreClientPort {

	ScoreInfo getScoreByCpf(String cpf);
}
