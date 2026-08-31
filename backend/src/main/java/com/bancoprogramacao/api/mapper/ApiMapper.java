package com.bancoprogramacao.api.mapper;

import com.bancoprogramacao.api.domain.Account;
import com.bancoprogramacao.api.domain.BankTransaction;
import com.bancoprogramacao.api.dto.AccountResponse;
import com.bancoprogramacao.api.dto.ClientResponse;
import com.bancoprogramacao.api.dto.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class ApiMapper {

    public AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
                account.getNumber(),
                account.getCheckDigit(),
                account.getAccountReference(),
                new ClientResponse(
                        account.getClient().getId(),
                        account.getClient().getFullName(),
                        account.getClient().getEmail(),
                        account.getClient().getPhone(),
                        account.getClient().getCreatedAt()
                ),
                account.getStatus(),
                account.isAdministrator(),
                account.getBalance(),
                account.getCreatedAt(),
                account.getClosedAt()
        );
    }

    public TransactionResponse toTransactionResponse(BankTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDirection(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getCounterpartyAccount(),
                transaction.getTransferId(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
