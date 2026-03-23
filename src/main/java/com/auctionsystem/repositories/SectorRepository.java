package com.auctionsystem.repositories;

import com.auctionsystem.entities.Sector;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Integer> {
}
