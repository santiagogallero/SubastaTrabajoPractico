package com.auctionsystem.auction;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubastaConfigExtRepository extends JpaRepository<SubastaConfigExt, Long> {
    Optional<SubastaConfigExt> findBySubastaId(Integer subastaId);
}
