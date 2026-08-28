package com.bancoprogramacao.api.service;

import com.bancoprogramacao.api.domain.Account;
import com.bancoprogramacao.api.domain.PasswordResetToken;
import com.bancoprogramacao.api.exception.BankBusinessException;
import com.bancoprogramacao.api.repository.AccountRepository;
import com.bancoprogramacao.api.repository.PasswordResetTokenRepository;
import com.bancoprogramacao.api.util.AccountReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetService {
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String mailFrom;

    public PasswordResetService(
            AccountRepository accountRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.mail-from}") String mailFrom
    ) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.mailFrom = mailFrom;
    }

    @Transactional
    public void requestReset(String accountValue, String emailValue) {
        AccountReference reference;
        try {
            reference = AccountReference.parse(accountValue);
        } catch (BankBusinessException exception) {
            return;
        }

        Account account = accountRepository.findByNumber(reference.number()).orElse(null);
        String email = emailValue.trim().toLowerCase(Locale.ROOT);
        if (account == null
                || !account.getCheckDigit().equals(reference.checkDigit())
                || account.getClient().getEmail() == null
                || !account.getClient().getEmail().equalsIgnoreCase(email)) {
            return;
        }

        tokenRepository.deleteUnusedByAccountId(account.getId());
        String rawToken = generateToken();
        tokenRepository.save(new PasswordResetToken(account, hash(rawToken), Instant.now().plus(TOKEN_LIFETIME)));
        sendEmail(account, email, rawToken);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHashForUpdate(hash(rawToken.trim()))
                .orElseThrow(this::invalidToken);
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(Instant.now())) {
            throw invalidToken();
        }

        token.getAccount().setPasswordHash(passwordEncoder.encode(newPassword));
        token.markUsed();
    }

    private void sendEmail(Account account, String email, String rawToken) {
        String link = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("resetToken", rawToken)
                .build().encode().toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Redefinição de senha - " + BankService.BANK_NAME);
        message.setText("Olá, " + account.getClient().getFullName() + ",\n\n"
                + "Use o link abaixo para criar uma nova senha. Ele expira em 30 minutos e só pode ser usado uma vez:\n\n"
                + link + "\n\nSe você não solicitou esta alteração, ignore esta mensagem.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new BankBusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o e-mail de recuperação. Tente novamente mais tarde.");
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível.", exception);
        }
    }

    private BankBusinessException invalidToken() {
        return new BankBusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                "O link de redefinição é inválido, expirou ou já foi utilizado.");
    }
}
