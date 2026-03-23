package com.auctionsystem.compliance;

import com.auctionsystem.compliance.dto.InicializarPagoRequest;
import com.auctionsystem.compliance.dto.PagoEstadoDto;
import com.auctionsystem.compliance.dto.RegistrarPagoRequest;
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
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceService complianceService;

    @PostMapping("/pagos/inicializar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<PagoEstadoDto> inicializarPago(@Valid @RequestBody InicializarPagoRequest request) {
        return ResponseEntity.ok(complianceService.inicializarPago(request.registroSubastaId()));
    }

    @PostMapping("/pagos/registrar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<PagoEstadoDto> registrarPago(@Valid @RequestBody RegistrarPagoRequest request) {
        return ResponseEntity.ok(complianceService.registrarPago(request.registroSubastaId()));
    }

    @PostMapping("/multas/procesar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<String> procesarMoras() {
        int total = complianceService.procesarMorasAhora();
        return ResponseEntity.ok("Moras procesadas: " + total);
    }

    @GetMapping("/mis-pagos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PagoEstadoDto>> misPagos(Principal principal) {
        return ResponseEntity.ok(complianceService.estadoUsuario(principal.getName()));
    }
}
