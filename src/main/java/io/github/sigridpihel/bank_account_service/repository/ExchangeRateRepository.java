package io.github.sigridpihel.bank_account_service.repository;

import io.github.sigridpihel.bank_account_service.model.entity.Currency;
import io.github.sigridpihel.bank_account_service.model.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByFromCurrencyAndToCurrency(Currency from, Currency to);
}
