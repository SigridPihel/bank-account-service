package io.github.sigridpihel.bank_account_service.model.entity;

import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

public class Balance {

    @Id
    private Long id;

    private Long accountId;

    private String currency;

     private BigDecimal amount;
}
