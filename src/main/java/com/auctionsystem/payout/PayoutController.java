package com.auctionsystem.payout;

import com.auctionsystem.payout.dto.CuentaCobroRequest;
import com.auctionsystem.payout.dto.CuentaCobroResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payout-accounts")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    @GetMapping
    @PreAuthorize("hasRole('POSTOR')")
    public ResponseEntity<List<CuentaCobroResponse>> listar(Principal principal) {
        return ResponseEntity.ok(payoutService.listar(principal.getName()));
    }

    @PostMapping
    @PreAuthorize("hasRole('POSTOR')")
    public ResponseEntity<CuentaCobroResponse> crear(
            Principal principal,
            @Valid @RequestBody CuentaCobroRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payoutService.crear(principal.getName(), request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('POSTOR')")
    public ResponseEntity<CuentaCobroResponse> actualizar(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody CuentaCobroRequest request
    ) {
        return ResponseEntity.ok(payoutService.actualizar(principal.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('POSTOR')")
    public ResponseEntity<Void> eliminar(Principal principal, @PathVariable Long id) {
        payoutService.eliminar(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
