package io.github.sigridpihel.bank_account_service.model.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ExchangeRequest(
        @NotBlank @Size(max = 34) String accountNumber,
        @NotBlank @Size(min = 3, max = 3) String fromCurrency,
        @NotBlank @Size(min = 3, max = 3) String toCurrency,
        @Positive @NotNull @Digits(integer = 17, fraction = 2) BigDecimal amount
) {}