package io.github.sigridpihel.bank_account_service.exception;

public class ExchangeRateNotFoundException extends RuntimeException {
    public ExchangeRateNotFoundException(String fromCurrency, String toCurrency) {
        super("No exchange rate found from " + fromCurrency + " to " + toCurrency);
    }
}
