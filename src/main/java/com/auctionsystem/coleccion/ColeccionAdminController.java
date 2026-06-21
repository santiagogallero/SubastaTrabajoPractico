package com.auctionsystem.coleccion;

import com.auctionsystem.coleccion.dto.AgregarProductoColeccionRequest;
import com.auctionsystem.coleccion.dto.ColeccionResponse;
import com.auctionsystem.coleccion.dto.CrearColeccionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/colecciones")
@RequiredArgsConstructor
public class ColeccionAdminController {

    private final ColeccionService coleccionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ColeccionResponse> crear(@Valid @RequestBody CrearColeccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coleccionService.crear(request));
    }

    @PostMapping("/{coleccionId}/productos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ColeccionResponse> agregarProducto(
            @PathVariable Integer coleccionId,
            @Valid @RequestBody AgregarProductoColeccionRequest request
    ) {
        return ResponseEntity.ok(coleccionService.agregarProducto(coleccionId, request));
    }

    @DeleteMapping("/{coleccionId}/productos/{productoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ColeccionResponse> quitarProducto(
            @PathVariable Integer coleccionId,
            @PathVariable Integer productoId
    ) {
        return ResponseEntity.ok(coleccionService.quitarProducto(coleccionId, productoId));
    }
}
