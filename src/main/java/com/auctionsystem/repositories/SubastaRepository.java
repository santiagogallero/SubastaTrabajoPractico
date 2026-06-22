package com.auctionsystem.repositories;

import com.auctionsystem.entities.Subasta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubastaRepository extends JpaRepository<Subasta, Integer> {

    @Query("""
            SELECT s FROM Subasta s
            WHERE s.categoria IS NULL
               OR UPPER(s.categoria) IN :categoriasPermitidas
            """)
    List<Subasta> findAllByCategoriasPermitidas(@Param("categoriasPermitidas") List<String> categoriasPermitidas);
}
