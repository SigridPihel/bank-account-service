package io.github.sigridpihel.bank_account_service.model.entity;

import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

public class ExchangeRate {

    @Id
    private Long id;

    private String fromCurrency;

    private String toCurrency;

    private BigDecimal rate;
}
