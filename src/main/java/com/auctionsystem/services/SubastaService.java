package com.auctionsystem.services;

import com.auctionsystem.entities.Subasta;
import com.auctionsystem.repositories.SubastaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubastaService {

    private final SubastaRepository subastaRepository;

    public List<Subasta> findAll() {
        return subastaRepository.findAll();
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
