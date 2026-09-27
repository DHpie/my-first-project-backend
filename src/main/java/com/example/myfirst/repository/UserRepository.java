package com.example.myfirst.repository;

import com.example.myfirst.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUuid(UUID uuid);
    boolean existsByUuid(UUID uuid);
    List<User> findByNicknameContainingAndIdNot(String nickname, Long excludeId, org.springframework.data.domain.Pageable pageable);
}
