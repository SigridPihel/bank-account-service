package io.github.sigridpihel.bank_account_service.exception;

public class SameCurrencyException extends RuntimeException {
    public SameCurrencyException(String currency) {
        super("Cannot exchange " + currency + " to " + currency + ", source and target currency must be different");
    }
}
