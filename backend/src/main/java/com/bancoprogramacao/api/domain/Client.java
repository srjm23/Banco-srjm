package com.bancoprogramacao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = true, unique = true, length = 254)
    private String email;

    @Column(nullable = true, unique = true, length = 11)
    private String phone;

    @Column(name = "cpf_encrypted", length = 128)
    private String cpfEncrypted;

    @Column(name = "cpf_hash", unique = true, length = 64)
    private String cpfHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Client() {
    }

    public Client(String fullName) {
        this(fullName, null, null);
    }

    public Client(String fullName, String email, String phone) {
        this(fullName, email, phone, null, null);
    }

    public Client(String fullName, String email, String phone, String cpfEncrypted, String cpfHash) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.cpfEncrypted = cpfEncrypted;
        this.cpfHash = cpfHash;
    }

    @PrePersist
    void prepareForInsert() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCpfEncrypted() { return cpfEncrypted; }
    public String getCpfHash() { return cpfHash; }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
