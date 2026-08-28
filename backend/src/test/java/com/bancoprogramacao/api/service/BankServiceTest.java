package com.bancoprogramacao.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bancoprogramacao.api.domain.Account;
import com.bancoprogramacao.api.domain.AccountStatus;
import com.bancoprogramacao.api.domain.Client;
import com.bancoprogramacao.api.dto.AccountCloseRequest;
import com.bancoprogramacao.api.exception.BankBusinessException;
import com.bancoprogramacao.api.repository.AccountRepository;
import com.bancoprogramacao.api.repository.BankTransactionRepository;
import com.bancoprogramacao.api.repository.ClientRepository;
import com.bancoprogramacao.api.repository.PixKeyRepository;
import com.bancoprogramacao.api.util.CheckDigitCalculator;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BankServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BankTransactionRepository transactionRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private SensitiveDataService sensitiveDataService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private BankService bankService;

    @BeforeEach
    void setUp() {
        bankService = new BankService(
                accountRepository,
                transactionRepository,
                clientRepository,
                pixKeyRepository,
                sensitiveDataService,
                passwordEncoder,
                "srjm"
        );
    }

    @Test
    void closesAnActiveAccountWithZeroBalanceAndValidPassword() {
        Account account = account("261533");
        String reference = account.getAccountReference();
        when(accountRepository.findByNumberForUpdate("261533")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("senha123", "hash-da-senha")).thenReturn(true);

        Account closedAccount = bankService.closeAccount(reference, new AccountCloseRequest("senha123"));

        assertThat(closedAccount.getStatus()).isEqualTo(AccountStatus.ENCERRADA);
        assertThat(closedAccount.getClosedAt()).isNotNull();
    }

    @Test
    void refusesToCloseAnAccountWithRemainingBalance() {
        Account account = account("4586");
        account.setBalance(new BigDecimal("10.00"));
        when(accountRepository.findByNumberForUpdate("4586")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("senha123", "hash-da-senha")).thenReturn(true);

        assertThatThrownBy(() -> bankService.closeAccount(
                account.getAccountReference(),
                new AccountCloseRequest("senha123")
        ))
                .isInstanceOf(BankBusinessException.class)
                .hasMessage("A conta só pode ser encerrada quando o saldo estiver zerado.");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ATIVA);
    }

    private Account account(String number) {
        return new Account(
                number,
                CheckDigitCalculator.calculate(number),
                new Client("Cliente de teste"),
                "hash-da-senha"
        );
    }
}
