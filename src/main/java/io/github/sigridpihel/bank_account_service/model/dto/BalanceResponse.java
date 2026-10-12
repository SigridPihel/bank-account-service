package io.github.sigridpihel.bank_account_service.model.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        String currency,
        BigDecimal balance
) {}
