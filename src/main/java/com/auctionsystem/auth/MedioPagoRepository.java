package com.auctionsystem.auth;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedioPagoRepository extends JpaRepository<MedioPago, Long> {
    List<MedioPago> findByUsuarioId(Long usuarioId);
    boolean existsByUsuarioIdAndVerificadoTrueAndActivoTrue(Long usuarioId);
}
