package com.auctionsystem.repositories;

import com.auctionsystem.entities.Asistente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsistenteRepository extends JpaRepository<Asistente, Integer> {
	Optional<Asistente> findByClienteIdAndSubastaId(Integer clienteId, Integer subastaId);

	long countBySubastaId(Integer subastaId);

	List<Asistente> findByClienteId(Integer clienteId);
}
