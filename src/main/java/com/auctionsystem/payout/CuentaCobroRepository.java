package com.auctionsystem.payout;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaCobroRepository extends JpaRepository<CuentaCobro, Long> {
    List<CuentaCobro> findByDuenioIdAndActivoTrueOrderByCreatedAtDesc(Integer duenioId);

    Optional<CuentaCobro> findByIdAndDuenioId(Long id, Integer duenioId);
}
