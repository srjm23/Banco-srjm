package com.bancoprogramacao.api.service;

import com.bancoprogramacao.api.domain.Account;
import com.bancoprogramacao.api.domain.AccountStatus;
import com.bancoprogramacao.api.domain.BankTransaction;
import com.bancoprogramacao.api.domain.Client;
import com.bancoprogramacao.api.domain.TransactionDirection;
import com.bancoprogramacao.api.domain.TransactionType;
import com.bancoprogramacao.api.dto.AccountCreateRequest;
import com.bancoprogramacao.api.dto.AccountCloseRequest;
import com.bancoprogramacao.api.dto.DepositRequest;
import com.bancoprogramacao.api.dto.PixRequest;
import com.bancoprogramacao.api.dto.WithdrawalRequest;
import com.bancoprogramacao.api.exception.BankBusinessException;
import com.bancoprogramacao.api.repository.AccountRepository;
import com.bancoprogramacao.api.repository.BankTransactionRepository;
import com.bancoprogramacao.api.repository.ClientRepository;
import com.bancoprogramacao.api.util.AccountReference;
import com.bancoprogramacao.api.util.CheckDigitCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BankService {

    public static final String BANK_NAME = "Banco SRJM";
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final int MIN_ACCOUNT_NUMBER = 100_000;
    private static final int ACCOUNT_NUMBER_RANGE = 900_000;
    private static final int ACCOUNT_GENERATION_ATTEMPTS = 100;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public BankService(
            AccountRepository accountRepository,
            BankTransactionRepository transactionRepository,
            ClientRepository clientRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Account createAccount(AccountCreateRequest request) {
        String number = generateUniqueAccountNumber();

        Client client = clientRepository.save(new Client(request.holderName().trim()));
        Account account = new Account(
                number,
                CheckDigitCalculator.calculate(number),
                client,
                passwordEncoder.encode(request.password()),
                request.administrator()
        );
        accountRepository.save(account);

        if (request.initialDeposit() != null) {
            recordTransaction(
                    account,
                    TransactionDirection.C,
                    TransactionType.DEPOSITO,
                    request.initialDeposit(),
                    null,
                    null,
                    "Depósito inicial de abertura de conta"
            );
        }
        return account;
    }

    public Account authenticate(String accountReference, String password) {
        Account account = getAccount(accountReference);
        requireValidPassword(account, password);
        if (account.getStatus() == AccountStatus.ENCERRADA) {
            throw conflict("A conta está encerrada e não permite acesso.");
        }
        return account;
    }

    public Account getAccount(String accountReference) {
        return loadAndValidateAccount(AccountReference.parse(accountReference));
    }

    public List<Account> getActiveAccounts() {
        return accountRepository.findByStatusOrderByCreatedAtDesc(AccountStatus.ATIVA);
    }

    @Transactional
    public Account updateAccountStatus(String accountReference, AccountStatus status) {
        Account account = loadAndValidateAccountForUpdate(AccountReference.parse(accountReference));
        if (account.getStatus() == AccountStatus.ENCERRADA) {
            throw conflict("A conta está encerrada e não pode ter o status alterado.");
        }
        if (status == AccountStatus.ENCERRADA) {
            throw invalid("Use a operação de encerramento para finalizar uma conta.");
        }
        account.setStatus(status);
        return account;
    }

    @Transactional
    public Account closeAccount(String accountReference, AccountCloseRequest request) {
        Account account = loadAndValidateAccountForUpdate(AccountReference.parse(accountReference));
        requireValidPassword(account, request.password());

        if (account.getStatus() == AccountStatus.ENCERRADA) {
            throw conflict("A conta já está encerrada.");
        }
        if (account.getStatus() == AccountStatus.BLOQUEADA) {
            throw conflict("Uma conta bloqueada não pode ser encerrada.");
        }
        if (money(account.getBalance()).compareTo(ZERO) != 0) {
            throw conflict("A conta só pode ser encerrada quando o saldo estiver zerado.");
        }

        account.close();
        return account;
    }

    @Transactional
    public OperationResult deposit(DepositRequest request) {
        Account account = loadAndValidateAccountForUpdate(AccountReference.parse(request.account()));
        requireActive(account);
        BankTransaction transaction = recordTransaction(
                account,
                TransactionDirection.C,
                TransactionType.DEPOSITO,
                request.amount(),
                null,
                null,
                "Depósito em dinheiro"
        );
        return new OperationResult(account, transaction);
    }

    @Transactional
    public OperationResult withdraw(WithdrawalRequest request) {
        Account account = loadAndValidateAccountForUpdate(AccountReference.parse(request.account()));
        requireActive(account);
        requireValidPassword(account, request.password());
        BankTransaction transaction = recordTransaction(
                account,
                TransactionDirection.D,
                TransactionType.SAQUE,
                request.amount(),
                null,
                null,
                "Saque em dinheiro"
        );
        return new OperationResult(account, transaction);
    }

    @Transactional
    public PixResult pix(PixRequest request) {
        AccountReference sourceReference = AccountReference.parse(request.sourceAccount());
        AccountReference destinationReference = AccountReference.parse(request.destinationAccount());
        if (sourceReference.number().equals(destinationReference.number())) {
            throw invalid("A conta de origem deve ser diferente da conta de destino.");
        }

        Collection<String> numbers = List.of(sourceReference.number(), destinationReference.number());
        Map<String, Account> accountsByNumber = accountRepository.findAllByNumbersForUpdate(numbers).stream()
                .collect(Collectors.toMap(Account::getNumber, Function.identity()));

        Account source = findLockedAccount(accountsByNumber, sourceReference);
        Account destination = findLockedAccount(accountsByNumber, destinationReference);
        requireActive(source);
        requireActive(destination);
        requireValidPassword(source, request.password());

        UUID transferId = UUID.randomUUID();
        BankTransaction debit = recordTransaction(
                source,
                TransactionDirection.D,
                TransactionType.PIX,
                request.amount(),
                destination.getAccountReference(),
                transferId,
                "PIX enviado"
        );
        BankTransaction credit = recordTransaction(
                destination,
                TransactionDirection.C,
                TransactionType.PIX,
                request.amount(),
                source.getAccountReference(),
                transferId,
                "PIX recebido"
        );

        return new PixResult(source, destination, debit, credit, transferId);
    }

    public StatementData getStatement(String accountReference, int limit, String order) {
        Account account = getAccount(accountReference);
        Sort.Direction direction;
        if ("asc".equalsIgnoreCase(order)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(order)) {
            direction = Sort.Direction.DESC;
        } else {
            throw invalid("A ordenação deve ser asc ou desc.");
        }

        Sort sort = Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
        List<BankTransaction> transactions = transactionRepository
                .findByAccountId(account.getId(), PageRequest.of(0, limit, sort))
                .getContent();
        return new StatementData(account, transactions);
    }

    private Account loadAndValidateAccount(AccountReference reference) {
        Account account = accountRepository.findByNumber(reference.number())
                .orElseThrow(() -> notFound("Conta não encontrada."));
        validateStoredCheckDigit(account, reference);
        return account;
    }

    private Account loadAndValidateAccountForUpdate(AccountReference reference) {
        Account account = accountRepository.findByNumberForUpdate(reference.number())
                .orElseThrow(() -> notFound("Conta não encontrada."));
        validateStoredCheckDigit(account, reference);
        return account;
    }

    private Account findLockedAccount(Map<String, Account> accountsByNumber, AccountReference reference) {
        Account account = accountsByNumber.get(reference.number());
        if (account == null) {
            throw notFound("Conta não encontrada.");
        }
        validateStoredCheckDigit(account, reference);
        return account;
    }

    private void validateStoredCheckDigit(Account account, AccountReference reference) {
        if (!account.getCheckDigit().equals(reference.checkDigit())) {
            throw invalid("Dígito verificador inválido para a conta informada.");
        }
    }

    private void requireActive(Account account) {
        if (account.getStatus() == AccountStatus.BLOQUEADA) {
            throw conflict("A conta está bloqueada e não pode realizar movimentações.");
        }
        if (account.getStatus() == AccountStatus.ENCERRADA) {
            throw conflict("A conta está encerrada e não pode realizar movimentações.");
        }
    }

    private void requireValidPassword(Account account, String password) {
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BankBusinessException(HttpStatus.UNAUTHORIZED, "Senha da conta inválida.");
        }
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < ACCOUNT_GENERATION_ATTEMPTS; attempt++) {
            String candidate = Integer.toString(MIN_ACCOUNT_NUMBER + SECURE_RANDOM.nextInt(ACCOUNT_NUMBER_RANGE));
            if (!accountRepository.existsByNumber(candidate)) {
                return candidate;
            }
        }
        throw conflict("Não foi possível gerar um número de conta único. Tente novamente.");
    }

    private BankTransaction recordTransaction(
            Account account,
            TransactionDirection direction,
            TransactionType transactionType,
            BigDecimal amount,
            String counterpartyAccount,
            UUID transferId,
            String description
    ) {
        BigDecimal normalizedAmount = money(amount);
        BigDecimal currentBalance = money(account.getBalance());
        BigDecimal nextBalance = direction == TransactionDirection.C
                ? currentBalance.add(normalizedAmount)
                : currentBalance.subtract(normalizedAmount);

        if (nextBalance.compareTo(ZERO) < 0) {
            throw invalid("Saldo insuficiente para concluir a operação.");
        }

        account.setBalance(nextBalance);
        BankTransaction transaction = new BankTransaction(
                account,
                direction,
                transactionType,
                normalizedAmount,
                nextBalance,
                counterpartyAccount,
                transferId,
                description
        );
        return transactionRepository.save(transaction);
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BankBusinessException invalid(String message) {
        return new BankBusinessException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private BankBusinessException notFound(String message) {
        return new BankBusinessException(HttpStatus.NOT_FOUND, message);
    }

    private BankBusinessException conflict(String message) {
        return new BankBusinessException(HttpStatus.CONFLICT, message);
    }

    public record OperationResult(Account account, BankTransaction transaction) {
    }

    public record PixResult(
            Account source,
            Account destination,
            BankTransaction debit,
            BankTransaction credit,
            UUID transferId
    ) {
    }

    public record StatementData(Account account, List<BankTransaction> transactions) {
    }
}
