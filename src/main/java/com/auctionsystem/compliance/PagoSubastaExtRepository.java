package com.auctionsystem.compliance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoSubastaExtRepository extends JpaRepository<PagoSubastaExt, Long> {
    Optional<PagoSubastaExt> findByRegistroSubastaId(Integer registroSubastaId);
    List<PagoSubastaExt> findByEstadoPagoAndFechaVencimientoBefore(String estadoPago, LocalDateTime fecha);
    List<PagoSubastaExt> findByEstadoPagoAndFechaVencimientoBetween(String estadoPago, LocalDateTime inicio, LocalDateTime fin);
    List<PagoSubastaExt> findByUsuarioIdOrderByFechaVencimientoDesc(Long usuarioId);
}
