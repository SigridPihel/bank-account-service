package io.github.sigridpihel.bank_account_service.repository;

import io.github.sigridpihel.bank_account_service.model.entity.Balance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceRepository extends JpaRepository<Balance, Long> {
}
