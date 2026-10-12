package io.github.sigridpihel.bank_account_service.exception;

public class InsufficientFundsException extends RuntimeException{
    public InsufficientFundsException(String accountNumber, String currency) {
        super("Insufficient funds in " + currency + " for account " + accountNumber);
    }
}
