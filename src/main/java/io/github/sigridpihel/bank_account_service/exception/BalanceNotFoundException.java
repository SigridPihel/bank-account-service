package io.github.sigridpihel.bank_account_service.exception;

public class BalanceNotFoundException extends RuntimeException {
    public BalanceNotFoundException(String accountNumber, String currency) {
        super("Account " + accountNumber + " has no balance in " + currency);
    }
}
