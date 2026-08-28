package com.bancoprogramacao.api.controller;

import com.bancoprogramacao.api.domain.BankTransaction;
import com.bancoprogramacao.api.domain.TransactionType;
import com.bancoprogramacao.api.dto.AccountCreateRequest;
import com.bancoprogramacao.api.dto.AccountCloseRequest;
import com.bancoprogramacao.api.dto.AccountClosureResponse;
import com.bancoprogramacao.api.dto.AccountResponse;
import com.bancoprogramacao.api.dto.AdminAccountResponse;
import com.bancoprogramacao.api.dto.AdminOverviewResponse;
import com.bancoprogramacao.api.dto.AccountStatusUpdateRequest;
import com.bancoprogramacao.api.dto.StatementResponse;
import com.bancoprogramacao.api.dto.TransactionResponse;
import com.bancoprogramacao.api.dto.PixKeyCreateRequest;
import com.bancoprogramacao.api.dto.PixKeyResponse;
import com.bancoprogramacao.api.mapper.ApiMapper;
import com.bancoprogramacao.api.service.BankService;
import com.bancoprogramacao.api.service.AccountSessionService;
import com.bancoprogramacao.api.service.StatementDocumentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/accounts")
public class AccountController {

    private static final ZoneId BRAZIL_TIME_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/yyyy")
            .withZone(BRAZIL_TIME_ZONE);

    private final BankService bankService;
    private final ApiMapper mapper;
    private final AccountSessionService accountSessionService;
    private final StatementDocumentService statementDocumentService;

    public AccountController(
            BankService bankService,
            ApiMapper mapper,
            AccountSessionService accountSessionService,
            StatementDocumentService statementDocumentService
    ) {
        this.bankService = bankService;
        this.mapper = mapper;
        this.accountSessionService = accountSessionService;
        this.statementDocumentService = statementDocumentService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody AccountCreateRequest request
    ) {
        AccountResponse response = mapper.toAccountResponse(bankService.createAccount(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountReference}")
    public AccountResponse getAccount(@PathVariable String accountReference, HttpSession session) {
        accountSessionService.requireOwnAccount(session, accountReference);
        return mapper.toAccountResponse(bankService.getAccount(accountReference));
    }

    @GetMapping("/{accountReference}/pix-keys")
    public List<PixKeyResponse> getPixKeys(@PathVariable String accountReference, HttpSession session) {
        accountSessionService.requireOwnAccount(session, accountReference);
        return bankService.getPixKeys(accountReference).stream()
                .map(key -> new PixKeyResponse(key.getType(), bankService.getPixKeyDisplayValue(key), key.getCreatedAt()))
                .toList();
    }

    @PostMapping("/{accountReference}/pix-keys")
    public ResponseEntity<PixKeyResponse> createPixKey(
            @PathVariable String accountReference,
            @Valid @RequestBody PixKeyCreateRequest request,
            HttpSession session
    ) {
        accountSessionService.requireOwnAccount(session, accountReference);
        var key = bankService.createPixKey(accountReference, request.type());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PixKeyResponse(key.getType(), bankService.getPixKeyDisplayValue(key), key.getCreatedAt()));
    }

    @GetMapping("/admin/all")
    public AdminOverviewResponse getAllAccounts(HttpSession session) {
        accountSessionService.requireAdministrator(session);
        List<AdminAccountResponse> accounts = bankService.getCustomerAccounts().stream()
                .map(account -> new AdminAccountResponse(
                        account.getAccountReference(),
                        account.getClient().getFullName(),
                        account.getClient().getEmail(),
                        account.getClient().getPhone(),
                        account.getClient().getCpfEncrypted(),
                        account.getStatus(),
                        account.getCreatedAt(),
                        account.getBalance()
                ))
                .toList();
        BigDecimal totalBalance = accounts.stream()
                .map(AdminAccountResponse::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AdminOverviewResponse(accounts.size(), totalBalance, accounts);
    }

    @PatchMapping("/{accountReference}/status")
    public AccountResponse updateAccountStatus(
            @PathVariable String accountReference,
            @Valid @RequestBody AccountStatusUpdateRequest request,
            HttpSession session
    ) {
        accountSessionService.requireOwnAccount(session, accountReference);
        return mapper.toAccountResponse(bankService.updateAccountStatus(accountReference, request.status()));
    }

    @PostMapping("/{accountReference}/close")
    public AccountClosureResponse closeAccount(
            @PathVariable String accountReference,
            @Valid @RequestBody AccountCloseRequest request,
            HttpSession session
    ) {
        accountSessionService.requireOwnAccount(session, accountReference);
        AccountResponse account = mapper.toAccountResponse(bankService.closeAccount(accountReference, request));
        return new AccountClosureResponse("Conta encerrada com sucesso.", account);
    }

    @GetMapping("/{accountReference}/statement")
    public StatementResponse getStatement(
            @PathVariable String accountReference,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            @RequestParam(defaultValue = "desc") String order,
            HttpSession session
    ) {
        accountSessionService.requireOwnAccount(session, accountReference);
        BankService.StatementData statement = bankService.getStatement(accountReference, limit, order);
        List<TransactionResponse> transactions = statement.transactions().stream()
                .map(mapper::toTransactionResponse)
                .toList();
        return new StatementResponse(
                BankService.BANK_NAME,
                java.time.Instant.now(),
                mapper.toAccountResponse(statement.account()),
                statement.account().getBalance(),
                transactions
        );
    }

    @GetMapping(value = "/{accountReference}/statement/text", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getTextStatement(
            @PathVariable String accountReference,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            @RequestParam(defaultValue = "desc") String order,
            HttpSession session
    ) {
        accountSessionService.requireOwnAccount(session, accountReference);
        BankService.StatementData statement = bankService.getStatement(accountReference, limit, order);
        return buildTextStatement(statement);
    }

    @GetMapping("/{accountReference}/statement/download")
    public ResponseEntity<byte[]> downloadStatement(
            @PathVariable String accountReference,
            @RequestParam String format,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            HttpSession session
    ) {
        accountSessionService.requireOwnAccount(session, accountReference);
        BankService.StatementData statement = bankService.getStatement(accountReference, limit, "desc");
        StatementDocumentService.Download download = statementDocumentService.generate(statement, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .contentType(MediaType.parseMediaType(download.mediaType()))
                .body(download.content());
    }

    private String buildTextStatement(BankService.StatementData statement) {
        AccountResponse account = mapper.toAccountResponse(statement.account());
        StringBuilder output = new StringBuilder();
        output.append(String.format(
                "%-38s%s%n",
                BankService.BANK_NAME,
                DATE_FORMATTER.format(java.time.Instant.now())
        ));
        output.append("-".repeat(64)).append(System.lineSeparator());
        output.append(account.accountReference())
                .append(" ")
                .append(account.client().fullName().toUpperCase(Locale.ROOT))
                .append(System.lineSeparator());
        output.append("Status: ").append(account.status()).append(System.lineSeparator());
        output.append("Saldo atual: ").append(formatMoney(account.balance())).append(System.lineSeparator());
        output.append(System.lineSeparator());

        for (BankTransaction transaction : statement.transactions()) {
            String label = transaction.getTransactionType().name();
            if (transaction.getTransactionType() == TransactionType.PIX) {
                label = "PIX " + transaction.getCounterpartyAccount();
            }
            output.append(String.format(
                    "%s %-24s %12s %12s%n",
                    transaction.getDirection().name(),
                    label,
                    formatMoney(transaction.getAmount()),
                    formatMoney(transaction.getBalanceAfter())
            ));
        }

        output.append(System.lineSeparator());
        output.append("-".repeat(64)).append(System.lineSeparator());
        output.append("Saldo: ").append(formatMoney(account.balance())).append(System.lineSeparator());
        return output.toString();
    }

    private String formatMoney(BigDecimal value) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new Locale("pt", "BR"));
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(value);
    }
}
