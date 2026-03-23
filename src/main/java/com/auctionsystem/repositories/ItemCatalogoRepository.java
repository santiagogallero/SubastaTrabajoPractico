package com.auctionsystem.repositories;

import com.auctionsystem.entities.ItemCatalogo;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemCatalogoRepository extends JpaRepository<ItemCatalogo, Integer> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from ItemCatalogo i where i.id = :id")
	Optional<ItemCatalogo> findByIdForUpdate(@Param("id") Integer id);
}
