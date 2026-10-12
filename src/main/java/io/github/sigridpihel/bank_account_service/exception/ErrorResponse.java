package io.github.sigridpihel.bank_account_service.exception;

import java.util.Map;

public record ErrorResponse(String message, String timestamp, Map<String, String> fieldErrors) {
}

