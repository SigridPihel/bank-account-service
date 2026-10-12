package io.github.sigridpihel.bank_account_service.service;

import io.github.sigridpihel.bank_account_service.exception.AccountNotFoundException;
import io.github.sigridpihel.bank_account_service.exception.BalanceNotFoundException;
import io.github.sigridpihel.bank_account_service.exception.ExchangeAmountTooSmallException;
import io.github.sigridpihel.bank_account_service.exception.ExchangeRateNotFoundException;
import io.github.sigridpihel.bank_account_service.exception.InsufficientFundsException;
import io.github.sigridpihel.bank_account_service.exception.UnsupportedCurrencyException;
import io.github.sigridpihel.bank_account_service.model.dto.BalanceResponse;
import io.github.sigridpihel.bank_account_service.model.dto.ExchangeRequest;
import io.github.sigridpihel.bank_account_service.model.dto.ExchangeResponse;
import io.github.sigridpihel.bank_account_service.model.dto.MoneyRequest;
import io.github.sigridpihel.bank_account_service.model.entity.Account;
import io.github.sigridpihel.bank_account_service.model.entity.Balance;
import io.github.sigridpihel.bank_account_service.model.entity.Currency;
import io.github.sigridpihel.bank_account_service.model.entity.ExchangeRate;
import io.github.sigridpihel.bank_account_service.model.entity.Transaction;
import io.github.sigridpihel.bank_account_service.model.enumeration.TransactionType;
import io.github.sigridpihel.bank_account_service.repository.AccountRepository;
import io.github.sigridpihel.bank_account_service.repository.BalanceRepository;
import io.github.sigridpihel.bank_account_service.repository.CurrencyRepository;
import io.github.sigridpihel.bank_account_service.repository.ExchangeRateRepository;
import io.github.sigridpihel.bank_account_service.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebitProcessorServiceTest {

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
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private DebitProcessorService debitProcessorService;

    private final Account account = account();
    private final Currency eur = currency("EUR");
    private final Currency usd = currency("USD");

    @Test
    void debit_subtractsAmount_andRecordsDebitTransaction() {
        Balance balance = new Balance(account, eur, new BigDecimal("100.00"));
        givenAccountAndCurrency(eur);
        when(balanceRepository.findByAccountAndCurrency(account, eur)).thenReturn(Optional.of(balance));

        BalanceResponse response = debitProcessorService.debit(new MoneyRequest(ACCOUNT_NUMBER, "EUR", new BigDecimal("30.00")));

        assertThat(response.balance()).isEqualByComparingTo("70.00");
        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(saved.capture());
        assertThat(saved.getValue().getTransactionType()).isEqualTo(TransactionType.DEBIT);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void debit_canUseTheWholeBalance() {
        Balance balance = new Balance(account, eur, new BigDecimal("30.00"));
        givenAccountAndCurrency(eur);
        when(balanceRepository.findByAccountAndCurrency(account, eur)).thenReturn(Optional.of(balance));

        BalanceResponse response = debitProcessorService.debit(new MoneyRequest(ACCOUNT_NUMBER, "EUR", new BigDecimal("30.00")));

        assertThat(response.balance()).isEqualByComparingTo("0.00");
    }

    @Test
    void debit_throws_whenFundsAreInsufficient_andChangesNothing() {
        Balance balance = new Balance(account, eur, new BigDecimal("20.00"));
        givenAccountAndCurrency(eur);
        when(balanceRepository.findByAccountAndCurrency(account, eur)).thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> debitProcessorService.debit(new MoneyRequest(ACCOUNT_NUMBER, "EUR", new BigDecimal("20.01"))))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(balance.getAmount()).isEqualByComparingTo("20.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void debit_throws_whenAccountHasNoBalanceInThatCurrency() {
        givenAccountAndCurrency(usd);
        when(balanceRepository.findByAccountAndCurrency(account, usd)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> debitProcessorService.debit(new MoneyRequest(ACCOUNT_NUMBER, "USD", BigDecimal.ONE)))
                .isInstanceOf(BalanceNotFoundException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void debit_throws_whenAccountDoesNotExist() {
        when(accountRepository.findByAccountNumber("EE000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> debitProcessorService.debit(new MoneyRequest("EE000", "EUR", BigDecimal.ONE)))
                .isInstanceOf(AccountNotFoundException.class);
        verifyNoInteractions(balanceRepository, transactionRepository);
    }

    @Test
    void exchange_canUseTheWholeBalance() {
        Balance eurBalance = new Balance(account, eur, new BigDecimal("50.00"));
        Balance usdBalance = new Balance(account, usd, BigDecimal.ZERO);
        givenExchangeSetup(new BigDecimal("1.080000"), eurBalance, usdBalance);

        ExchangeResponse response = debitProcessorService.exchange(
                new ExchangeRequest(ACCOUNT_NUMBER, "EUR", "USD", new BigDecimal("50.00")));

        assertThat(response.from().balance()).isEqualByComparingTo("0.00");
        assertThat(response.to().balance()).isEqualByComparingTo("54.00");
    }

    @Test
    void exchange_throws_whenConvertedAmountRoundsToZero_andChangesNothing() {
        Currency sek = currency("SEK");
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(currencyRepository.findByCode("SEK")).thenReturn(Optional.of(sek));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(sek, eur))
                .thenReturn(Optional.of(exchangeRate(new BigDecimal("0.087719"))));

        assertThatThrownBy(() -> debitProcessorService.exchange(
                new ExchangeRequest(ACCOUNT_NUMBER, "SEK", "EUR", new BigDecimal("0.01"))))
                .isInstanceOf(ExchangeAmountTooSmallException.class);
        verifyNoInteractions(balanceRepository, transactionRepository);
    }

    @Test
    void exchange_throws_whenExchangeRateIsMissing() {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usd));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(eur, usd)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> debitProcessorService.exchange(
                new ExchangeRequest(ACCOUNT_NUMBER, "EUR", "USD", BigDecimal.TEN)))
                .isInstanceOf(ExchangeRateNotFoundException.class);
        verifyNoInteractions(balanceRepository, transactionRepository);
    }

    @Test
    void exchange_throws_whenTargetCurrencyIsNotSupported() {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));
        when(currencyRepository.findByCode("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> debitProcessorService.exchange(
                new ExchangeRequest(ACCOUNT_NUMBER, "EUR", "XYZ", BigDecimal.TEN)))
                .isInstanceOf(UnsupportedCurrencyException.class);
        verifyNoInteractions(exchangeRateRepository, balanceRepository, transactionRepository);
    }

    @Test
    void exchange_convertsWithRate_andUpdatesBothBalances() {
        Balance eurBalance = new Balance(account, eur, new BigDecimal("200.00"));
        Balance usdBalance = new Balance(account, usd, new BigDecimal("5.00"));
        givenExchangeSetup(new BigDecimal("1.080000"), eurBalance, usdBalance);

        ExchangeResponse response = debitProcessorService.exchange(
                new ExchangeRequest(ACCOUNT_NUMBER, "EUR", "USD", new BigDecimal("100.00")));

        assertThat(response.convertedAmount()).isEqualByComparingTo("108.00");
        assertThat(response.rate()).isEqualByComparingTo("1.08");
        assertThat(response.from().balance()).isEqualByComparingTo("100.00");
        assertThat(response.to().balance()).isEqualByComparingTo("113.00");
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void exchange_createsTargetBalance_whenItDoesNotExist() {
        Balance eurBalance = new Balance(account, eur, new BigDecimal("50.00"));
        givenExchangeSetup(new BigDecimal("1.080000"), eurBalance, null);
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExchangeResponse response = debitProcessorService.exchange(
                new ExchangeRequest(ACCOUNT_NUMBER, "EUR", "USD", new BigDecimal("50.00")));

        assertThat(response.to().currency()).isEqualTo("USD");
        assertThat(response.to().balance()).isEqualByComparingTo("54.00");
        verify(balanceRepository).save(any(Balance.class));
    }

    @Test
    void exchange_roundsConvertedAmountToCents_withHalfEven() {
        Balance eurBalance = new Balance(account, eur, new BigDecimal("100.00"));
        Balance usdBalance = new Balance(account, usd, BigDecimal.ZERO);
        givenExchangeSetup(new BigDecimal("0.500000"), eurBalance, usdBalance);

        ExchangeResponse response = debitProcessorService.exchange(
                new ExchangeRequest(ACCOUNT_NUMBER, "EUR", "USD", new BigDecimal("10.05")));

        assertThat(response.convertedAmount()).isEqualByComparingTo("5.02");
        assertThat(response.convertedAmount().scale()).isEqualTo(2);
    }

    @Test
    void exchange_throws_whenFundsAreInsufficient_andTargetIsNotCredited() {
        Balance eurBalance = new Balance(account, eur, new BigDecimal("10.00"));
        Balance usdBalance = new Balance(account, usd, new BigDecimal("5.00"));
        givenExchangeSetup(new BigDecimal("1.080000"), eurBalance, usdBalance);

        assertThatThrownBy(() -> debitProcessorService.exchange(
                new ExchangeRequest(ACCOUNT_NUMBER, "EUR", "USD", new BigDecimal("10.01"))))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(eurBalance.getAmount()).isEqualByComparingTo("10.00");
        assertThat(usdBalance.getAmount()).isEqualByComparingTo("5.00");
        verify(transactionRepository, never()).save(any());
    }

    private void givenAccountAndCurrency(Currency currency) {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(currencyRepository.findByCode(currency.getCode())).thenReturn(Optional.of(currency));
    }

    private void givenExchangeSetup(BigDecimal rate, Balance fromBalance, Balance toBalance) {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usd));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(eur, usd)).thenReturn(Optional.of(exchangeRate(rate)));
        when(balanceRepository.findByAccountAndCurrency(account, eur)).thenReturn(Optional.of(fromBalance));
        when(balanceRepository.findByAccountAndCurrency(account, usd)).thenReturn(Optional.ofNullable(toBalance));
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

    private static ExchangeRate exchangeRate(BigDecimal rate) {
        ExchangeRate exchangeRate = new ExchangeRate();
        ReflectionTestUtils.setField(exchangeRate, "rate", rate);
        return exchangeRate;
    }
}
