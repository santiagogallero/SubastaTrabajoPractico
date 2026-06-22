package com.auctionsystem.services;

import com.auctionsystem.entities.Subasta;
import com.auctionsystem.repositories.SubastaRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubastaService {

    private static final List<String> RANKING = List.of("COMUN", "ESPECIAL", "PLATA", "ORO", "PLATINO");

    private final SubastaRepository subastaRepository;

    public List<Subasta> findAll() {
        return subastaRepository.findAll();
    }

    public List<Subasta> findAllAccesibles(String categoriaCliente) {
        int rankCliente = rankCategoria(categoriaCliente);
        List<String> permitidas = RANKING.subList(0, rankCliente);
        return subastaRepository.findAllByCategoriasPermitidas(permitidas);
    }

    private int rankCategoria(String categoria) {
        if (categoria == null) return 0;
        int idx = RANKING.indexOf(categoria.toUpperCase(Locale.ROOT));
        return idx >= 0 ? idx + 1 : 0;
    }

    public Optional<Subasta> findById(Integer id) {
        return subastaRepository.findById(id);
    }

    public Subasta save(Subasta subasta) {
        return subastaRepository.save(subasta);
    }

    public void deleteById(Integer id) {
        subastaRepository.deleteById(id);
    }
}
