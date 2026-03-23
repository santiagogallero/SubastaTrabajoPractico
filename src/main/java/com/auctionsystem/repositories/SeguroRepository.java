package com.auctionsystem.repositories;

import com.auctionsystem.entities.Seguro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeguroRepository extends JpaRepository<Seguro, String> {
}
