package com.example.banking.lib;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.banking.models.AuthUserEntity;

@Repository
public interface AuthUserJpaRepository extends JpaRepository<AuthUserEntity, String> {
    boolean existsByEmail(String email);

    Optional<AuthUserEntity> findByEmail(String email);
}
