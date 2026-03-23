package com.auctionsystem.services;

import com.auctionsystem.entities.Persona;
import com.auctionsystem.repositories.PersonaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaRepository personaRepository;

    public List<Persona> findAll() {
        return personaRepository.findAll();
    }

    public Optional<Persona> findById(Integer id) {
        return personaRepository.findById(id);
    }

    public Persona save(Persona persona) {
        return personaRepository.save(persona);
    }

    public void deleteById(Integer id) {
        personaRepository.deleteById(id);
    }
}
