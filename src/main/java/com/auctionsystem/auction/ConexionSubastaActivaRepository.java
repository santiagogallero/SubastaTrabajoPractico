package com.auctionsystem.auction;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConexionSubastaActivaRepository extends JpaRepository<ConexionSubastaActiva, Long> {
    Optional<ConexionSubastaActiva> findByClienteId(Integer clienteId);
    void deleteByClienteId(Integer clienteId);
}
