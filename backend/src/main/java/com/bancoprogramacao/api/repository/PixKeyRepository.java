package com.bancoprogramacao.api.repository;

import com.bancoprogramacao.api.domain.PixKey;
import com.bancoprogramacao.api.domain.PixKeyType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PixKeyRepository extends JpaRepository<PixKey, Long> {
    List<PixKey> findByAccountIdOrderByCreatedAtAsc(Long accountId);
    Optional<PixKey> findByValue(String value);
    boolean existsByAccountIdAndType(Long accountId, PixKeyType type);
    boolean existsByValue(String value);

    @Query("select key from PixKey key where key.type = 'CPF' and key.account.client.cpfHash = :cpfHash")
    Optional<PixKey> findCpfKeyByClientCpfHash(@Param("cpfHash") String cpfHash);
}
