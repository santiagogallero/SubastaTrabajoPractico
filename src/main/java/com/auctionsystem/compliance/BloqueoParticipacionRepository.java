package com.auctionsystem.compliance;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloqueoParticipacionRepository extends JpaRepository<BloqueoParticipacion, Long> {
    Optional<BloqueoParticipacion> findByUsuarioId(Long usuarioId);
    boolean existsByUsuarioIdAndActivoTrue(Long usuarioId);
}
