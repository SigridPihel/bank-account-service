package io.github.sigridpihel.bank_account_service.model.dto;

import java.math.BigDecimal;

public record ExchangeResponse(
        BalanceResponse from,
        BalanceResponse to,
        BigDecimal rate,
        BigDecimal convertedAmount
) {}