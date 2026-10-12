package io.github.sigridpihel.bank_account_service.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
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

    public Balance(Account account, Currency currency, BigDecimal amount) {
        this.account = account;
        this.currency = currency;
        this.amount = amount;
    }
}
