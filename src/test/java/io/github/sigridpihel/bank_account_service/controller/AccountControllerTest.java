package io.github.sigridpihel.bank_account_service.controller;

import io.github.sigridpihel.bank_account_service.exception.AccountNotFoundException;
import io.github.sigridpihel.bank_account_service.exception.ExternalLoggingException;
import io.github.sigridpihel.bank_account_service.exception.InsufficientFundsException;
import io.github.sigridpihel.bank_account_service.exception.SameCurrencyException;
import io.github.sigridpihel.bank_account_service.model.dto.BalanceResponse;
import io.github.sigridpihel.bank_account_service.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AccountController.class)
class AccountControllerTest {

    private static final String ACCOUNT_NUMBER = "EE382200221020145685";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void credit_returns200_withNewBalance() throws Exception {
        when(accountService.credit(any())).thenReturn(new BalanceResponse("EUR", new BigDecimal("150.00")));

        mockMvc.perform(post("/api/accounts/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moneyJson(ACCOUNT_NUMBER, "EUR", "100.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void credit_returns400_withFieldErrors_whenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/accounts/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moneyJson("", "EURO", "-5.123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body contains invalid fields"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors.accountNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.currency").exists())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        verifyNoInteractions(accountService);
    }

    @Test
    void debit_returns400_whenAmountIsZero() throws Exception {
        mockMvc.perform(post("/api/accounts/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moneyJson(ACCOUNT_NUMBER, "EUR", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").value("must be greater than 0"));

        verifyNoInteractions(accountService);
    }

    @Test
    void credit_returns400_whenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/accounts/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": \"abc\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void getBalances_returns400_whenAccountNumberParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/accounts/balances"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required query parameter 'accountNumber' is missing"));
    }

    @Test
    void getBalances_returns400_withFieldError_whenAccountNumberIsBlank() throws Exception {
        mockMvc.perform(get("/api/accounts/balances").param("accountNumber", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.accountNumber").value("must not be blank"));

        verifyNoInteractions(accountService);
    }

    @Test
    void debit_returns404_whenAccountDoesNotExist() throws Exception {
        when(accountService.debit(any())).thenThrow(new AccountNotFoundException("EE000"));

        mockMvc.perform(post("/api/accounts/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moneyJson("EE000", "EUR", "10.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found: EE000"));
    }

    @Test
    void debit_returns422_whenFundsAreInsufficient() throws Exception {
        when(accountService.debit(any())).thenThrow(new InsufficientFundsException(ACCOUNT_NUMBER, "EUR"));

        mockMvc.perform(post("/api/accounts/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moneyJson(ACCOUNT_NUMBER, "EUR", "999.00")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Insufficient funds in EUR for account " + ACCOUNT_NUMBER));
    }

    @Test
    void exchange_returns400_whenCurrenciesAreTheSame() throws Exception {
        when(accountService.exchangeCurrencies(any())).thenThrow(new SameCurrencyException("EUR"));

        mockMvc.perform(post("/api/accounts/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountNumber": "%s", "fromCurrency": "EUR", "toCurrency": "EUR", "amount": 10.00}
                                """.formatted(ACCOUNT_NUMBER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void debit_returns503_whenExternalLoggingFails() throws Exception {
        when(accountService.debit(any())).thenThrow(new ExternalLoggingException(new RuntimeException("timeout")));

        mockMvc.perform(post("/api/accounts/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moneyJson(ACCOUNT_NUMBER, "EUR", "10.00")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void unexpectedError_returns500_withoutInternalDetails() throws Exception {
        when(accountService.credit(any())).thenThrow(new IllegalStateException("database password is wrong"));

        mockMvc.perform(post("/api/accounts/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moneyJson(ACCOUNT_NUMBER, "EUR", "10.00")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    private static String moneyJson(String accountNumber, String currency, String amount) {
        return """
                {"accountNumber": "%s", "currency": "%s", "amount": %s}
                """.formatted(accountNumber, currency, amount);
    }
}
