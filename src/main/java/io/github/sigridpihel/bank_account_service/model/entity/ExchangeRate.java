package io.github.sigridpihel.bank_account_service.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Currency fromCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    private Currency toCurrency;

    private BigDecimal rate;
}
