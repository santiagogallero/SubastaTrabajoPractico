package com.auctionsystem.auction;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubastaConfigExtRepository extends JpaRepository<SubastaConfigExt, Long> {
    Optional<SubastaConfigExt> findBySubastaId(Integer subastaId);

    @Query("select c from SubastaConfigExt c where c.subasta.estado = 'ABIERTA' and c.itemActual is not null")
    List<SubastaConfigExt> findSubastasActivasConItem();
}
