package io.github.sigridpihel.bank_account_service.exception;

public class ExternalLoggingException extends RuntimeException{
    public ExternalLoggingException(Throwable cause) {
        super("External logging service is unavailable, debit was not executed", cause);
    }
}
