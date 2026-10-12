package io.github.sigridpihel.bank_account_service.model.entity;

import io.github.sigridpihel.bank_account_service.model.enumeration.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Balance balance;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    private BigDecimal amount;

    private Instant createdAt;

    public Transaction(Balance balance, TransactionType transactionType, BigDecimal amount) {
        this.balance = balance;
        this.transactionType = transactionType;
        this.amount = amount;
        this.createdAt = Instant.now();
    }
}
