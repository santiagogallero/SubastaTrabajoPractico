package com.auctionsystem.repositories;

import com.auctionsystem.entities.Catalogo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoRepository extends JpaRepository<Catalogo, Integer> {
}
