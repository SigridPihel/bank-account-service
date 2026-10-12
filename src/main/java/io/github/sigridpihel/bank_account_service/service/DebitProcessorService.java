package io.github.sigridpihel.bank_account_service.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class DebitProcessorService {

    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;
    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    @Transactional
    public BalanceResponse debit(MoneyRequest request) {
        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.accountNumber()));

        Currency currency = currencyRepository.findByCode(request.currency())
                .orElseThrow(() -> new UnsupportedCurrencyException(request.currency()));

        Balance balance = balanceRepository.findByAccountAndCurrency(account, currency)
                .orElseThrow(() -> new BalanceNotFoundException(request.accountNumber(), request.currency()));

        if (request.amount().compareTo(balance.getAmount()) > 0 ) {
            throw new InsufficientFundsException(request.accountNumber(), request.currency());
        }

        balance.setAmount(balance.getAmount().subtract(request.amount()));

        transactionRepository.save(new Transaction(balance, TransactionType.DEBIT, request.amount()));

        return new BalanceResponse(currency.getCode(), balance.getAmount());
    }

    @Transactional
    public ExchangeResponse exchange(ExchangeRequest request) {
        Account account = getAccount(request.accountNumber());
        Currency from = getCurrency(request.fromCurrency());
        Currency to = getCurrency(request.toCurrency());

        BigDecimal rate = exchangeRateRepository.findByFromCurrencyAndToCurrency(from, to)
                .orElseThrow(() -> new ExchangeRateNotFoundException(from.getCode(), to.getCode())).getRate();

        BigDecimal converted = request.amount().multiply(rate).setScale(2, RoundingMode.HALF_EVEN);

        if (converted.signum() == 0) {
            throw new ExchangeAmountTooSmallException(request.amount(), from.getCode(), to.getCode());
        }

        Balance fromBalance = lockExistingBalance(account, from);
        Balance toBalance = lockOrCreateBalance(account, to);

        withdraw(fromBalance, request.amount());
        deposit(toBalance, converted);

        return new ExchangeResponse(toResponse(fromBalance), toResponse(toBalance), rate, converted);
    }

    private Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private Currency getCurrency(String code) {
        return currencyRepository.findByCode(code)
                .orElseThrow(() -> new UnsupportedCurrencyException(code));
    }

    private Balance lockExistingBalance(Account account, Currency currency) {
        return balanceRepository.findByAccountAndCurrency(account, currency)
                .orElseThrow(() -> new BalanceNotFoundException(account.getAccountNumber(), currency.getCode()));
    }

    private Balance lockOrCreateBalance(Account account, Currency currency) {
        return balanceRepository.findByAccountAndCurrency(account, currency)
                .orElseGet(() -> balanceRepository.save(new Balance(account, currency, BigDecimal.ZERO)));
    }

    private void withdraw(Balance balance, BigDecimal amount) {
        if (amount.compareTo(balance.getAmount()) > 0) {
            throw new InsufficientFundsException(
                    balance.getAccount().getAccountNumber(), balance.getCurrency().getCode());
        }
        balance.setAmount(balance.getAmount().subtract(amount));
        transactionRepository.save(new Transaction(balance, TransactionType.DEBIT, amount));
    }

    private void deposit(Balance balance, BigDecimal amount) {
        balance.setAmount(balance.getAmount().add(amount));
        transactionRepository.save(new Transaction(balance, TransactionType.CREDIT, amount));
    }

    private BalanceResponse toResponse(Balance balance) {
        return new BalanceResponse(balance.getCurrency().getCode(), balance.getAmount());
    }
}
