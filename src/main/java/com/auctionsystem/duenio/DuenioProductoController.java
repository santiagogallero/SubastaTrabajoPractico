package com.auctionsystem.duenio;

import com.auctionsystem.duenio.dto.DuenioProductoRequest;
import com.auctionsystem.duenio.dto.DuenioProductoResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/duenio")
@RequiredArgsConstructor
public class DuenioProductoController {

    private final DuenioProductoService duenioProductoService;

    @PostMapping("/productos")
    @PreAuthorize("hasRole('DUENIO')")
    public ResponseEntity<DuenioProductoResponse> registrarProducto(
            @Valid @RequestBody DuenioProductoRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(duenioProductoService.registrarProducto(principal.getName(), request));
    }

    @GetMapping("/productos")
    @PreAuthorize("hasRole('DUENIO')")
    public ResponseEntity<List<DuenioProductoResponse>> listarMisProductos(Principal principal) {
        return ResponseEntity.ok(duenioProductoService.listarProductos(principal.getName()));
    }
}
