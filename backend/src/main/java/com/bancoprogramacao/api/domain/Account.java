package com.bancoprogramacao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 7)
    private String number;

    @Column(name = "check_digit", nullable = false, length = 1)
    private String checkDigit;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(nullable = false)
    private boolean administrator;

    protected Account() {
    }

    public Account(String number, String checkDigit, Client client, String passwordHash) {
        this(number, checkDigit, client, passwordHash, false);
    }

    public Account(String number, String checkDigit, Client client, String passwordHash, boolean administrator) {
        this.number = number;
        this.checkDigit = checkDigit;
        this.client = client;
        this.passwordHash = passwordHash;
        this.status = AccountStatus.ATIVA;
        this.balance = new BigDecimal("0.00");
        this.administrator = administrator;
    }

    @PrePersist
    void prepareForInsert() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (balance == null) {
            balance = new BigDecimal("0.00");
        }
        if (status == null) {
            status = AccountStatus.ATIVA;
        }
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getCheckDigit() {
        return checkDigit;
    }

    public Client getClient() {
        return client;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public boolean isAdministrator() {
        return administrator;
    }

    public String getAccountReference() {
        return number + "-" + checkDigit;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void close() {
        this.status = AccountStatus.ENCERRADA;
        this.closedAt = Instant.now();
    }
}
