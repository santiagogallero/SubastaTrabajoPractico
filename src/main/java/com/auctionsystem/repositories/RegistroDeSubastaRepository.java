package com.auctionsystem.repositories;

import com.auctionsystem.entities.RegistroDeSubasta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroDeSubastaRepository extends JpaRepository<RegistroDeSubasta, Integer> {
	List<RegistroDeSubasta> findByClienteId(Integer clienteId);
}
