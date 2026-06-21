package com.auctionsystem.repositories;

import com.auctionsystem.entities.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByDuenioIdOrderByIdDesc(Integer duenioId);

    List<Producto> findByEstadoInspeccionOrderByIdAsc(String estadoInspeccion);

    List<Producto> findByDuenioIdAndSeguroIsNotNullOrderByIdDesc(Integer duenioId);
}
