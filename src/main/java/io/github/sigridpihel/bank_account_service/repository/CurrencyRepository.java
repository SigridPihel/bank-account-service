package io.github.sigridpihel.bank_account_service.repository;

import io.github.sigridpihel.bank_account_service.model.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, String> {
}
