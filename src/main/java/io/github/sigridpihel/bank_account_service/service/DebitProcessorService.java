package io.github.sigridpihel.bank_account_service.service;

import io.github.sigridpihel.bank_account_service.exception.AccountNotFoundException;
import io.github.sigridpihel.bank_account_service.exception.BalanceNotFoundException;
import io.github.sigridpihel.bank_account_service.exception.InsufficientFundsException;
import io.github.sigridpihel.bank_account_service.exception.UnsupportedCurrencyException;
import io.github.sigridpihel.bank_account_service.model.dto.BalanceResponse;
import io.github.sigridpihel.bank_account_service.model.dto.MoneyRequest;
import io.github.sigridpihel.bank_account_service.model.entity.Account;
import io.github.sigridpihel.bank_account_service.model.entity.Balance;
import io.github.sigridpihel.bank_account_service.model.entity.Currency;
import io.github.sigridpihel.bank_account_service.model.entity.Transaction;
import io.github.sigridpihel.bank_account_service.model.enumeration.TransactionType;
import io.github.sigridpihel.bank_account_service.repository.AccountRepository;
import io.github.sigridpihel.bank_account_service.repository.BalanceRepository;
import io.github.sigridpihel.bank_account_service.repository.CurrencyRepository;
import io.github.sigridpihel.bank_account_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DebitProcessorService {

    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;
    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;

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
}
