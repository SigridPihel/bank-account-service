package io.github.sigridpihel.bank_account_service.service;

import io.github.sigridpihel.bank_account_service.client.ExternalLoggingClient;
import io.github.sigridpihel.bank_account_service.exception.*;
import io.github.sigridpihel.bank_account_service.model.dto.BalanceResponse;
import io.github.sigridpihel.bank_account_service.model.dto.ExchangeRequest;
import io.github.sigridpihel.bank_account_service.model.dto.ExchangeResponse;
import io.github.sigridpihel.bank_account_service.model.dto.MoneyRequest;
import io.github.sigridpihel.bank_account_service.model.entity.Account;
import io.github.sigridpihel.bank_account_service.model.entity.Balance;
import io.github.sigridpihel.bank_account_service.model.entity.Currency;
import io.github.sigridpihel.bank_account_service.model.entity.Transaction;
import io.github.sigridpihel.bank_account_service.model.enumeration.TransactionType;
import io.github.sigridpihel.bank_account_service.repository.*;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;
    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    private final ExternalLoggingClient externalLoggingClient;
    private final DebitProcessorService debitProcessorService;

    @Transactional
    public BalanceResponse credit(MoneyRequest request) {
        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.accountNumber()));

        Currency currency = currencyRepository.findByCode(request.currency())
                .orElseThrow(() -> new UnsupportedCurrencyException(request.currency()));

        Balance balance = balanceRepository.findByAccountAndCurrency(account, currency)
                .orElseGet(() -> balanceRepository.save(new Balance(account, currency, BigDecimal.ZERO)));

        balance.setAmount(balance.getAmount().add(request.amount()));

        transactionRepository.save(new Transaction(balance, TransactionType.CREDIT, request.amount()));

        return new BalanceResponse(currency.getCode(), balance.getAmount());
    }


    public BalanceResponse debit(MoneyRequest request) {
        externalLoggingClient.log();
        return debitProcessorService.debit(request);
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getBalances(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        return account.getBalances().stream()
                .map(balance -> new BalanceResponse(balance.getCurrency().getCode(), balance.getAmount()))
                .toList();
    }

    public ExchangeResponse exchangeCurrencies(ExchangeRequest request) {
        if (request.fromCurrency().equals(request.toCurrency())) {
            throw new SameCurrencyException(request.fromCurrency());
        }

        externalLoggingClient.log();

        return debitProcessorService.exchange(request);
    }
}
