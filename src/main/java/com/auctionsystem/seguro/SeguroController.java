package com.auctionsystem.seguro;

import com.auctionsystem.seguro.dto.SeguroPolizaResponse;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Polizas de seguro sobre articulos del postor autenticado.
 */
@RestController
@RequestMapping("/api/seguros")
@RequiredArgsConstructor
public class SeguroController {

    private final SeguroService seguroService;

    @GetMapping("/mios")
    @PreAuthorize("hasRole('POSTOR')")
    public ResponseEntity<List<SeguroPolizaResponse>> misPolizas(Principal principal) {
        return ResponseEntity.ok(seguroService.misPolizas(principal.getName()));
    }
}
