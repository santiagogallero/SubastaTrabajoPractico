package com.auctionsystem.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
    List<EmailVerificationCode> findByEmailAndUsedFalse(String email);
    Optional<EmailVerificationCode> findTopByEmailOrderByCreatedAtDesc(String email);
}
