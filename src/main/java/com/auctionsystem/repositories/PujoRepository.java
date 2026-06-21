package com.auctionsystem.repositories;

import com.auctionsystem.entities.Pujo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PujoRepository extends JpaRepository<Pujo, Integer> {
	Optional<Pujo> findTopByItemIdOrderByImporteDesc(Integer itemId);
	List<Pujo> findByItemIdOrderByIdAsc(Integer itemId);
	List<Pujo> findByAsistenteClienteId(Integer clienteId);
}
