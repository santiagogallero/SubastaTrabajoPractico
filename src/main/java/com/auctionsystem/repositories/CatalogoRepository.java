package com.auctionsystem.repositories;

import com.auctionsystem.entities.Catalogo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoRepository extends JpaRepository<Catalogo, Integer> {

    Optional<Catalogo> findFirstBySubasta_Id(Integer subastaId);
}
