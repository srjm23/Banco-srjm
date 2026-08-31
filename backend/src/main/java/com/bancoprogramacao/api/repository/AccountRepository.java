package com.bancoprogramacao.api.repository;

import com.bancoprogramacao.api.domain.Account;
import com.bancoprogramacao.api.domain.AccountStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByNumber(String number);

    Optional<Account> findByNumber(String number);

    @Query("select account from Account account where account.client.cpfHash = :cpfHash")
    Optional<Account> findByClientCpfHash(@Param("cpfHash") String cpfHash);

    List<Account> findByStatusOrderByCreatedAtDesc(AccountStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from Account account where account.number = :number")
    Optional<Account> findByNumberForUpdate(@Param("number") String number);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from Account account where account.number in :numbers order by account.number asc")
    List<Account> findAllByNumbersForUpdate(@Param("numbers") Collection<String> numbers);
}
