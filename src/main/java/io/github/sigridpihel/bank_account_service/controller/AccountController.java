package io.github.sigridpihel.bank_account_service.controller;

import io.github.sigridpihel.bank_account_service.model.dto.BalanceResponse;
import io.github.sigridpihel.bank_account_service.model.dto.MoneyRequest;
import io.github.sigridpihel.bank_account_service.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/credit")
    @ResponseStatus(HttpStatus.OK)
    public BalanceResponse credit(
            @Valid @RequestBody MoneyRequest request) {
        return accountService.credit(request);
    }

    @PostMapping("/debit")
    @ResponseStatus(HttpStatus.OK)
    public BalanceResponse debit(
            @Valid @RequestBody MoneyRequest request) {
        return accountService.debit(request);
    }
}
