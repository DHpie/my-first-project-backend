package com.example.myfirst.repository;

import com.example.myfirst.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("SELECT ub FROM UserBlock ub WHERE ub.blockerId = :blockerId")
    List<UserBlock> findByBlockerId(@Param("blockerId") Long blockerId);

    @Query("SELECT ub FROM UserBlock ub WHERE ub.blockedId = :blockedId")
    List<UserBlock> findByBlockedId(@Param("blockedId") Long blockedId);
}
