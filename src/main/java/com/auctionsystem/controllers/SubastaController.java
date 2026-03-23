package com.auctionsystem.controllers;

import com.auctionsystem.entities.Subasta;
import com.auctionsystem.services.SubastaService;
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
@RequestMapping("/api/subastas")
@RequiredArgsConstructor
public class SubastaController {

    private final SubastaService subastaService;

    @GetMapping
    public List<Subasta> findAll() {
        return subastaService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subasta> findById(@PathVariable Integer id) {
        return subastaService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Subasta create(@RequestBody Subasta subasta) {
        return subastaService.save(subasta);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Subasta> update(@PathVariable Integer id, @RequestBody Subasta subasta) {
        if (subastaService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        subasta.setId(id);
        return ResponseEntity.ok(subastaService.save(subasta));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (subastaService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        subastaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
