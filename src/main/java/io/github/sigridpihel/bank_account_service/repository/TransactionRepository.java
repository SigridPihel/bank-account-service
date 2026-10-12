package io.github.sigridpihel.bank_account_service.repository;

import io.github.sigridpihel.bank_account_service.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
