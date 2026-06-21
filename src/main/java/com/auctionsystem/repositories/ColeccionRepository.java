package com.auctionsystem.repositories;

import com.auctionsystem.entities.Coleccion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColeccionRepository extends JpaRepository<Coleccion, Integer> {

    Optional<Coleccion> findBySubastaId(Integer subastaId);

    List<Coleccion> findAllByOrderByIdAsc();
}
