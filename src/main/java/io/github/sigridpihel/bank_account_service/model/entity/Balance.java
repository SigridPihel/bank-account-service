package io.github.sigridpihel.bank_account_service.model.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
public class Balance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    private Currency currency;

    private BigDecimal amount;

    @OneToMany(mappedBy = "balance")
    private List<Transaction> transactions;
}
