package com.auctionsystem.repositories;

import com.auctionsystem.entities.Foto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FotoRepository extends JpaRepository<Foto, Integer> {

    long countByProductoId(Integer productoId);

    java.util.Optional<Foto> findFirstByProductoIdOrderByIdAsc(Integer productoId);
}
