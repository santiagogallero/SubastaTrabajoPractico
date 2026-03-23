package com.auctionsystem.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroPostorRepository extends JpaRepository<RegistroPostor, Long> {
    Optional<RegistroPostor> findByUsuarioId(Long usuarioId);
}
