package com.auctionsystem.auction;

import com.auctionsystem.auction.dto.AdjudicacionResponse;
import com.auctionsystem.auction.dto.EntregaRequest;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auction-runtime")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping("/items/{itemId}/cerrar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO','SUBASTADOR')")
    public ResponseEntity<AdjudicacionResponse> cerrarItem(@PathVariable Integer itemId) {
        return ResponseEntity.ok(saleService.cerrarItem(itemId));
    }

    @GetMapping("/mis-adjudicaciones")
    @PreAuthorize("hasRole('POSTOR')")
    public ResponseEntity<List<AdjudicacionResponse>> misAdjudicaciones(Principal principal) {
        return ResponseEntity.ok(saleService.misAdjudicaciones(principal.getName()));
    }

    @PostMapping("/adjudicaciones/{registroId}/entrega")
    @PreAuthorize("hasRole('POSTOR')")
    public ResponseEntity<AdjudicacionResponse> seleccionarEntrega(
            @PathVariable Integer registroId,
            @RequestBody EntregaRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(saleService.seleccionarEntrega(principal.getName(), registroId, request));
    }
}
