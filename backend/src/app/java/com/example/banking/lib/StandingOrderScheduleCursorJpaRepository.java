package com.example.banking.lib;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.StandingOrderScheduleCursorEntity;

public interface StandingOrderScheduleCursorJpaRepository extends JpaRepository<StandingOrderScheduleCursorEntity, String> {
}
