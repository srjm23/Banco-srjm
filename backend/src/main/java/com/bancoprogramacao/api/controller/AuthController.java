package com.bancoprogramacao.api.controller;

import com.bancoprogramacao.api.dto.AccountResponse;
import com.bancoprogramacao.api.dto.LoginRequest;
import com.bancoprogramacao.api.mapper.ApiMapper;
import com.bancoprogramacao.api.service.AccountSessionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AccountSessionService accountSessionService;
    private final ApiMapper mapper;

    public AuthController(AccountSessionService accountSessionService, ApiMapper mapper) {
        this.accountSessionService = accountSessionService;
        this.mapper = mapper;
    }

    @PostMapping("/login")
    public AccountResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        return mapper.toAccountResponse(accountSessionService.login(session, request.account(), request.password()));
    }

    @GetMapping("/me")
    public AccountResponse currentAccount(HttpSession session) {
        return mapper.toAccountResponse(accountSessionService.currentAccount(session));
    }

    @DeleteMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        accountSessionService.logout(session);
    }
}
