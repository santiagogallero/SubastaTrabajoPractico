package com.auctionsystem.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioAuthRepository extends JpaRepository<UsuarioAuth, Long> {
    Optional<UsuarioAuth> findByEmail(String email);
    Optional<UsuarioAuth> findByPersonaId(Integer personaId);
    boolean existsByEmail(String email);
}
