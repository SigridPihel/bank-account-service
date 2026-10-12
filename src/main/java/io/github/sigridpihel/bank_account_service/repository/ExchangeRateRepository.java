package io.github.sigridpihel.bank_account_service.repository;

import io.github.sigridpihel.bank_account_service.model.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
}
