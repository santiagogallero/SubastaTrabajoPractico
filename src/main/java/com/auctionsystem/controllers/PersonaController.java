package com.auctionsystem.controllers;

import com.auctionsystem.entities.Persona;
import com.auctionsystem.services.PersonaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;

    @GetMapping
    public List<Persona> findAll() {
        return personaService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Persona> findById(@PathVariable Integer id) {
        return personaService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Persona create(@RequestBody Persona persona) {
        return personaService.save(persona);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Persona> update(@PathVariable Integer id, @RequestBody Persona persona) {
        if (personaService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        persona.setId(id);
        return ResponseEntity.ok(personaService.save(persona));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (personaService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        personaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
