package io.github.sigridpihel.bank_account_service.service;

import io.github.sigridpihel.bank_account_service.client.ExternalLoggingClient;
import io.github.sigridpihel.bank_account_service.exception.AccountNotFoundException;
import io.github.sigridpihel.bank_account_service.exception.ExternalLoggingException;
import io.github.sigridpihel.bank_account_service.exception.SameCurrencyException;
import io.github.sigridpihel.bank_account_service.exception.UnsupportedCurrencyException;
import io.github.sigridpihel.bank_account_service.model.dto.BalanceResponse;
import io.github.sigridpihel.bank_account_service.model.dto.ExchangeRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String ACCOUNT_NUMBER = "EE382200221020145685";

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private BalanceRepository balanceRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ExternalLoggingClient externalLoggingClient;
    @Mock
    private DebitProcessorService debitProcessorService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void credit_createsBalance_whenAccountHasNoBalanceInThatCurrency() {
        Account account = account();
        Currency eur = currency("EUR");
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));
        when(balanceRepository.findByAccountAndCurrency(account, eur)).thenReturn(Optional.empty());
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BalanceResponse response = accountService.credit(new MoneyRequest(ACCOUNT_NUMBER, "EUR", new BigDecimal("100.00")));

        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.balance()).isEqualByComparingTo("100.00");
        verify(balanceRepository).save(any(Balance.class));
    }

    @Test
    void credit_addsToExistingBalance_andRecordsCreditTransaction() {
        Account account = account();
        Currency eur = currency("EUR");
        Balance balance = new Balance(account, eur, new BigDecimal("50.00"));
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));
        when(balanceRepository.findByAccountAndCurrency(account, eur)).thenReturn(Optional.of(balance));

        BalanceResponse response = accountService.credit(new MoneyRequest(ACCOUNT_NUMBER, "EUR", new BigDecimal("25.50")));

        assertThat(response.balance()).isEqualByComparingTo("75.50");
        assertThat(balance.getAmount()).isEqualByComparingTo("75.50");
        verify(balanceRepository, never()).save(any(Balance.class));

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(saved.capture());
        assertThat(saved.getValue().getTransactionType()).isEqualTo(TransactionType.CREDIT);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("25.50");
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void credit_throws_whenAccountDoesNotExist() {
        when(accountRepository.findByAccountNumber("EE000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.credit(new MoneyRequest("EE000", "EUR", BigDecimal.TEN)))
                .isInstanceOf(AccountNotFoundException.class);
        verifyNoInteractions(balanceRepository, transactionRepository);
    }

    @Test
    void credit_throws_whenCurrencyIsNotSupported() {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account()));
        when(currencyRepository.findByCode("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.credit(new MoneyRequest(ACCOUNT_NUMBER, "XYZ", BigDecimal.TEN)))
                .isInstanceOf(UnsupportedCurrencyException.class);
        verifyNoInteractions(balanceRepository, transactionRepository);
    }

    @Test
    void getBalances_returnsAllBalancesOfTheAccount() {
        Account account = account();
        account.setBalances(List.of(
                new Balance(account, currency("EUR"), new BigDecimal("100.00")),
                new Balance(account, currency("SEK"), new BigDecimal("5.00"))));
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        List<BalanceResponse> balances = accountService.getBalances(ACCOUNT_NUMBER);

        assertThat(balances).extracting(BalanceResponse::currency).containsExactly("EUR", "SEK");
        assertThat(balances).extracting(BalanceResponse::balance).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("5.00"));
    }

    @Test
    void getBalances_returnsEmptyList_whenAccountHasNoBalances() {
        Account account = account();
        account.setBalances(List.of());
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        assertThat(accountService.getBalances(ACCOUNT_NUMBER)).isEmpty();
    }

    @Test
    void getBalances_throws_whenAccountDoesNotExist() {
        when(accountRepository.findByAccountNumber("EE000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getBalances("EE000"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void debit_callsExternalLoggingBeforeDebiting() {
        MoneyRequest request = new MoneyRequest(ACCOUNT_NUMBER, "EUR", BigDecimal.TEN);

        accountService.debit(request);

        InOrder order = inOrder(externalLoggingClient, debitProcessorService);
        order.verify(externalLoggingClient).log();
        order.verify(debitProcessorService).debit(request);
    }

    @Test
    void debit_isNotExecuted_whenExternalLoggingFails() {
        doThrow(new ExternalLoggingException(new RuntimeException("503"))).when(externalLoggingClient).log();

        assertThatThrownBy(() -> accountService.debit(new MoneyRequest(ACCOUNT_NUMBER, "EUR", BigDecimal.TEN)))
                .isInstanceOf(ExternalLoggingException.class);
        verifyNoInteractions(debitProcessorService);
    }

    @Test
    void exchange_throws_whenCurrenciesAreTheSame() {
        ExchangeRequest request = new ExchangeRequest(ACCOUNT_NUMBER, "EUR", "EUR", BigDecimal.TEN);

        assertThatThrownBy(() -> accountService.exchangeCurrencies(request))
                .isInstanceOf(SameCurrencyException.class);
        verifyNoInteractions(externalLoggingClient, debitProcessorService);
    }

    @Test
    void exchange_isNotExecuted_whenExternalLoggingFails() {
        doThrow(new ExternalLoggingException(new RuntimeException("timeout"))).when(externalLoggingClient).log();
        ExchangeRequest request = new ExchangeRequest(ACCOUNT_NUMBER, "EUR", "USD", BigDecimal.TEN);

        assertThatThrownBy(() -> accountService.exchangeCurrencies(request))
                .isInstanceOf(ExternalLoggingException.class);
        verifyNoInteractions(debitProcessorService);
    }

    private static Account account() {
        Account account = new Account();
        account.setAccountNumber(ACCOUNT_NUMBER);
        return account;
    }

    private static Currency currency(String code) {
        Currency currency = new Currency();
        ReflectionTestUtils.setField(currency, "code", code);
        return currency;
    }
}
