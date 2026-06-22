package com.auctionsystem.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
    List<EmailVerificationCode> findByEmailAndTypeAndUsedFalse(String email, String type);
    Optional<EmailVerificationCode> findTopByEmailAndTypeOrderByCreatedAtDesc(String email, String type);
}
