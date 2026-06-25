package com.example.banking.lib;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean emailExists(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM auth_users WHERE email = ?",
                Integer.class,
                normalizeEmail(email));
        return count != null && count > 0;
    }

    public void createUser(UUID userId, String email, String passwordHash) {
        jdbcTemplate.update(
                "INSERT INTO auth_users (id, email, password_hash) VALUES (?, ?, ?)",
                userId.toString(),
                normalizeEmail(email),
                passwordHash);
    }

    public Optional<String> findPasswordHashByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT password_hash FROM auth_users WHERE email = ?",
                (rs, rowNum) -> rs.getString("password_hash"),
                normalizeEmail(email)).stream().findFirst();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
