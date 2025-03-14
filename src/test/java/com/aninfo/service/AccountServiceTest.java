package com.aninfo.service;

import com.aninfo.exceptions.AccountNotFoundException;
import com.aninfo.exceptions.AlreadyExistsExeption;
import com.aninfo.exceptions.DepositNegativeSumException;
import com.aninfo.exceptions.InsufficientFundsException;
import com.aninfo.model.Account;
import com.aninfo.model.TransactionType;
import com.aninfo.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.aninfo.model.TransactionType.DEPOSIT;
import static com.aninfo.model.TransactionType.WITHDRAWAL;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class AccountServiceTest {

    private static final Long CBU = 1234567890L;
    private static final String ALIAS = "drebrown";

    @InjectMocks
    private AccountService accountService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private Account account, otherAccount, expectedAccount;

    @Captor
    private ArgumentCaptor<Double> balanceArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> nameArgumentCaptor;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    void createAccount() {
        when(this.account.getCbu()).thenReturn(CBU);
        when(this.account.getAlias()).thenReturn(ALIAS);
        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.empty());
        when(this.accountRepository.findAccountByAlias(ALIAS)).thenReturn(Optional.empty());
        when(this.accountRepository.save(this.account)).thenReturn(this.expectedAccount);

        Account actualAccount = this.accountService.createAccount(this.account);

        assertSame(this.expectedAccount, actualAccount);
        verify(this.accountRepository).findAccountByCbu(CBU);
        verify(this.accountRepository).findAccountByAlias(ALIAS);
        verify(this.accountRepository).save(this.account);
    }

    @Test
    void createAccount_alreadyExistsCbuThrowsAlreadyExistsException() {
        when(this.account.getCbu()).thenReturn(CBU);
        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.of(mock(Account.class)));

        AlreadyExistsExeption exception = assertThrows(AlreadyExistsExeption.class,
                () -> this.accountService.createAccount(this.account));
        assertEquals("The CBU already exists.", exception.getMessage());
    }

    @Test
    void createAccount_alreadyExistsAliasThrowsAlreadyExistsException() {
        when(this.account.getCbu()).thenReturn(CBU);
        when(this.account.getAlias()).thenReturn(ALIAS);
        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.empty());
        when(this.accountRepository.findAccountByAlias(ALIAS)).thenReturn(Optional.of(mock(Account.class)));

        AlreadyExistsExeption exception = assertThrows(AlreadyExistsExeption.class,
                () -> this.accountService.createAccount(this.account));
        assertEquals("The Alias already exists.", exception.getMessage());
    }

    @Test
    void getAccounts() {
        // Repository without accounts
        when(this.accountRepository.findAll()).thenReturn(new ArrayList<>());
        assertTrue(this.accountService.getAccounts().isEmpty());

        // Repository with only one account
        when(this.accountRepository.findAll()).thenReturn(List.of(account));
        Collection<Account> accounts = this.accountService.getAccounts();

        assertEquals(1, accounts.size());
        assertTrue(accounts.contains(account));

        // Repository with two accounts
        when(this.accountRepository.findAll()).thenReturn(List.of(account, otherAccount));
        accounts = this.accountService.getAccounts();

        assertEquals(2, accounts.size());
        assertTrue(accounts.contains(account));
        assertTrue(accounts.contains(otherAccount));

        verify(this.accountRepository, times(3)).findAll();
    }

    @Test
    void findByCbu() {
        // Didn't find any account
        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.empty());
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> this.accountService.findByCbu(CBU));
        assertEquals("The CBU does not have any related account.", exception.getMessage());

        // Actually find some account
        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.of(this.expectedAccount));
        Account actualAccount = this.accountService.findByCbu(CBU);
        assertSame(this.expectedAccount, actualAccount);

        verify(this.accountRepository, times(2)).findAccountByCbu(CBU);
    }

    @Test
    void update() {
        // Didn't find any account to update
        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.empty());
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> this.accountService.update(CBU, this.account));
        assertEquals("The CBU does not have any related account.", exception.getMessage());

        // Updating the account
        String name = "Some account name";
        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.of(this.otherAccount));
        when(this.otherAccount.getName()).thenReturn(name);
        when(this.accountRepository.save(this.account)).thenReturn(any(Account.class));

        this.accountService.update(CBU, this.account);

        verify(this.accountRepository, times(2)).findAccountByCbu(CBU);
        verify(this.account).setCbu(CBU);
        verify(this.account).setName(name);
        verify(this.accountRepository).save(this.account);
    }

    @Test
    void save() {
        when(this.accountRepository.save(this.account)).thenReturn(any(Account.class));

        this.accountService.save(this.account);

        verify(this.accountRepository).save(this.account);
    }

    @Test
    void deleteByCbu() {
        Long id = 123L;
        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.of(this.account));
        when(this.account.getId()).thenReturn(id);
        doNothing().when(this.accountRepository).deleteById(id);

        this.accountService.deleteByCbu(CBU);

        verify(this.accountRepository).findAccountByCbu(CBU);
        verify(this.accountRepository).deleteById(id);
    }

    @Test
    void deleteByCbu_accountNotFoundThrowsAccountNotFoundException() {
        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.empty());

        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> this.accountService.deleteByCbu(CBU));
        assertEquals("The CBU does not have any related account.", exception.getMessage());
    }

    @Test
    void withdraw_accountNotFoundThrowsAccountNotFoundException() {
        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.empty());

        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> this.accountService.withdraw(CBU, 1200D));
        assertEquals("The CBU does not have any related account.", exception.getMessage());
    }

    @Test
    void withdraw_withInsufficientFundsThrowsInsufficientFundsException() {
        Double sum = 1200D;
        Double balance = 10D;

        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.of(this.account));
        when(this.account.getBalance()).thenReturn(balance);

        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
                () -> this.accountService.withdraw(CBU, sum));
        assertEquals("Insufficient funds", exception.getMessage());
    }

    @Test
    void withdraw_sameSumAndBalanceLeftAccountWithBalanceInZero() {
        Double sum = 1200D;
        Double balance = 1200D;
        Double expectedBalance = 0D;

        testOperationOverAccount(WITHDRAWAL, sum, balance, expectedBalance);
    }

    @Test
    void withdraw() {
        Double sum = 200D;
        Double balance = 1200D;
        Double expectedBalance = 1000D;

        testOperationOverAccount(WITHDRAWAL, sum, balance, expectedBalance);
    }

    @Test
    void deposit_withoutSumToAddThrowsDepositNegativeSumException() {
        DepositNegativeSumException exception = assertThrows(DepositNegativeSumException.class,
                () -> this.accountService.deposit(CBU, 0D));
        assertEquals("Cannot deposit negative sums", exception.getMessage());
    }

    @Test
    void deposit_withNegativeSumToAddThrowsDepositNegativeSumException() {
        DepositNegativeSumException exception = assertThrows(DepositNegativeSumException.class,
                () -> this.accountService.deposit(CBU, -10D));
        assertEquals("Cannot deposit negative sums", exception.getMessage());
    }

    @Test
    void deposit_accountNotFoundThrowsAccountNotFoundException() {
        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.empty());

        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> this.accountService.deposit(CBU, 1200D));
        assertEquals("The CBU does not have any related account.", exception.getMessage());
    }

    @Test
    void deposit() {
        Double sum = 200D;
        Double balance = 1200D;
        Double expectedBalance = 1400D;

        testOperationOverAccount(DEPOSIT, sum, balance, expectedBalance);
    }

    private void testOperationOverAccount(TransactionType transactionType,
                                          Double sum,
                                          Double balance,
                                          Double expectedBalance) {
        when(this.accountRepository.findAccountByCbu(CBU)).thenReturn(Optional.of(this.account));
        when(this.account.getBalance()).thenReturn(balance);
        when(this.accountRepository.save(this.account)).thenReturn(this.expectedAccount);

        Account actualAccount = this.doOperation(transactionType, sum);

        assertSame(this.expectedAccount, actualAccount);
        verify(this.account).setBalance(this.balanceArgumentCaptor.capture());
        assertEquals(expectedBalance, this.balanceArgumentCaptor.getValue());
        verify(this.accountRepository).findAccountByCbu(CBU);
        verify(this.accountRepository).save(account);
    }

    private Account doOperation(TransactionType transactionType, Double sum) {
        switch (transactionType) {
            case DEPOSIT:
                return this.accountService.deposit(CBU, sum);
            case WITHDRAWAL:
                return this.accountService.withdraw(CBU, sum);
            default:
                return null;
        }
    }
}
