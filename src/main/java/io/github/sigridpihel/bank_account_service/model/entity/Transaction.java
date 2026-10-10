package io.github.sigridpihel.bank_account_service.model.entity;

import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.Instant;

public class Transaction {

    @Id
    private Long id;

    private Long balanceId;

    private String transactionType;

    private BigDecimal amount;

    private Instant createdAt;
}
