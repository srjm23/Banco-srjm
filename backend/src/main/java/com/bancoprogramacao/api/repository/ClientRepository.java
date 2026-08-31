package com.bancoprogramacao.api.repository;

import com.bancoprogramacao.api.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByCpfHash(String cpfHash);
}
