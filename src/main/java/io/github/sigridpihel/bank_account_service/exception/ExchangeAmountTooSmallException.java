package io.github.sigridpihel.bank_account_service.exception;

import java.math.BigDecimal;

public class ExchangeAmountTooSmallException extends RuntimeException {
    public ExchangeAmountTooSmallException(BigDecimal amount, String fromCurrency, String toCurrency) {
        super("Amount " + amount + " " + fromCurrency + " is too small to exchange to " + toCurrency
                + ", converted amount would be less than 0.01");
    }
}
