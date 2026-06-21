package com.auctionsystem.coleccion;

import com.auctionsystem.coleccion.dto.ColeccionResponse;
import com.auctionsystem.coleccion.dto.ColeccionResumenResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/colecciones")
@RequiredArgsConstructor
public class ColeccionController {

    private final ColeccionService coleccionService;

    @GetMapping
    public List<ColeccionResumenResponse> listar() {
        return coleccionService.listarResumen();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ColeccionResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(coleccionService.obtener(id));
    }

    @GetMapping("/por-subasta/{subastaId}")
    public ResponseEntity<ColeccionResponse> porSubasta(@PathVariable Integer subastaId) {
        return ResponseEntity.ok(coleccionService.obtenerPorSubasta(subastaId));
    }
}
