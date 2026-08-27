package com.bancoprogramacao.api.service;

import com.bancoprogramacao.api.domain.Account;
import com.bancoprogramacao.api.exception.BankBusinessException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AccountSessionService {

    private static final String AUTHENTICATED_ACCOUNT = "authenticatedAccount";

    private final BankService bankService;

    public AccountSessionService(BankService bankService) {
        this.bankService = bankService;
    }

    public Account login(HttpSession session, String accountReference, String password) {
        Account account = bankService.authenticate(accountReference, password);
        session.setAttribute(AUTHENTICATED_ACCOUNT, account.getAccountReference());
        return account;
    }

    public Account currentAccount(HttpSession session) {
        return bankService.getAccount(requireReference(session));
    }

    public void requireOwnAccount(HttpSession session, String accountReference) {
        String authenticatedReference = requireReference(session);
        if (!authenticatedReference.equals(accountReference == null ? "" : accountReference.trim())) {
            throw new BankBusinessException(
                    HttpStatus.FORBIDDEN,
                    "A operação só pode ser realizada pela conta autenticada."
            );
        }
    }

    public void requireAdministrator(HttpSession session) {
        Account account = currentAccount(session);
        if (!account.isAdministrator()) {
            throw new BankBusinessException(HttpStatus.FORBIDDEN, "Acesso exclusivo para contas administradoras.");
        }
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    private String requireReference(HttpSession session) {
        Object value = session.getAttribute(AUTHENTICATED_ACCOUNT);
        if (!(value instanceof String reference) || reference.isBlank()) {
            throw new BankBusinessException(HttpStatus.UNAUTHORIZED, "Faça login para acessar esta operação.");
        }
        return reference;
    }
}
