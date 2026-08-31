package com.bancoprogramacao.api.controller;

import com.bancoprogramacao.api.dto.DepositRequest;
import com.bancoprogramacao.api.dto.OperationResponse;
import com.bancoprogramacao.api.dto.PaymentRequest;
import com.bancoprogramacao.api.dto.PixRequest;
import com.bancoprogramacao.api.dto.PixResponse;
import com.bancoprogramacao.api.dto.PixRecipientResponse;
import com.bancoprogramacao.api.mapper.ApiMapper;
import com.bancoprogramacao.api.service.BankService;
import com.bancoprogramacao.api.service.AccountSessionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final BankService bankService;
    private final ApiMapper mapper;
    private final AccountSessionService accountSessionService;

    public TransactionController(BankService bankService, ApiMapper mapper, AccountSessionService accountSessionService) {
        this.bankService = bankService;
        this.mapper = mapper;
        this.accountSessionService = accountSessionService;
    }

    @PostMapping("/deposits")
    public ResponseEntity<OperationResponse> deposit(@Valid @RequestBody DepositRequest request, HttpSession session) {
        accountSessionService.requireOwnAccount(session, request.account());
        BankService.OperationResult result = bankService.deposit(request);
        OperationResponse response = new OperationResponse(
                "Depósito realizado com sucesso.",
                mapper.toAccountResponse(result.account()),
                mapper.toTransactionResponse(result.transaction())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/payments")
    public ResponseEntity<OperationResponse> pay(@Valid @RequestBody PaymentRequest request, HttpSession session) {
        accountSessionService.requireOwnAccount(session, request.account());
        BankService.OperationResult result = bankService.pay(request);
        OperationResponse response = new OperationResponse(
                "Pagamento realizado com sucesso.",
                mapper.toAccountResponse(result.account()),
                mapper.toTransactionResponse(result.transaction())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/pix")
    public ResponseEntity<PixResponse> pix(@Valid @RequestBody PixRequest request, HttpSession session) {
        accountSessionService.requireOwnAccount(session, request.sourceAccount());
        BankService.PixResult result = bankService.pix(request);
        PixResponse response = new PixResponse(
                "PIX realizado com sucesso.",
                result.transferId(),
                mapper.toAccountResponse(result.source()),
                mapper.toAccountResponse(result.destination()),
                mapper.toTransactionResponse(result.debit()),
                mapper.toTransactionResponse(result.credit())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/pix/recipient")
    public PixRecipientResponse getPixRecipient(@RequestParam String key, HttpSession session) {
        accountSessionService.currentAccount(session);
        var recipient = bankService.getPixRecipient(key);
        return new PixRecipientResponse(recipient.getClient().getFullName());
    }
}
