package com.bancoprogramacao.api.repository;

import com.bancoprogramacao.api.domain.BankTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    Page<BankTransaction> findByAccountId(Long accountId, Pageable pageable);
}

