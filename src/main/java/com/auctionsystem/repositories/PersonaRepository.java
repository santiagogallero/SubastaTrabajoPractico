package com.auctionsystem.repositories;

import com.auctionsystem.entities.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, Integer> {
	boolean existsByDocumento(String documento);
}
