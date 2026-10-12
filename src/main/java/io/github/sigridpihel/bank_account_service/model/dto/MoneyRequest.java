package io.github.sigridpihel.bank_account_service.model.dto;

import io.github.sigridpihel.bank_account_service.model.entity.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record MoneyRequest(
        @NotBlank @Size(max = 34) String accountNumber,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Positive @NotNull @Digits(integer = 17, fraction = 2) BigDecimal amount
) {}
