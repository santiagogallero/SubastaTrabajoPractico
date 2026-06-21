package com.auctionsystem.product;

import com.auctionsystem.product.dto.ArticuloResponse;
import com.auctionsystem.product.dto.InspeccionRequest;
import com.auctionsystem.product.dto.PublicarArticuloRequest;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del flujo de venta de articulos propios con inspeccion.
 *
 * <ul>
 *   <li>POST   /api/articulos              - publicar un articulo (POSTOR).</li>
 *   <li>GET    /api/articulos/mios         - mis articulos publicados (POSTOR).</li>
 *   <li>GET    /api/articulos/pendientes   - cola de inspeccion (EMPLEADO/ADMIN/MODERADOR).</li>
 *   <li>POST   /api/articulos/{id}/inspeccion - aprobar/rechazar (EMPLEADO/ADMIN/MODERADOR).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/articulos")
@RequiredArgsConstructor
public class ArticuloController {

    private final ArticuloService articuloService;

    @PostMapping
    @PreAuthorize("hasRole('POSTOR')")
    public ResponseEntity<ArticuloResponse> publicar(Principal principal,
            @RequestBody PublicarArticuloRequest request) {
        ArticuloResponse creado = articuloService.publicar(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/mios")
    @PreAuthorize("hasRole('POSTOR')")
    public ResponseEntity<List<ArticuloResponse>> misArticulos(Principal principal) {
        return ResponseEntity.ok(articuloService.misArticulos(principal.getName()));
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('EMPLEADO','ADMIN','MODERADOR')")
    public ResponseEntity<List<ArticuloResponse>> pendientes() {
        return ResponseEntity.ok(articuloService.pendientesDeInspeccion());
    }

    @PostMapping("/{id}/inspeccion")
    @PreAuthorize("hasAnyRole('EMPLEADO','ADMIN','MODERADOR')")
    public ResponseEntity<ArticuloResponse> inspeccionar(Principal principal,
            @PathVariable Integer id, @RequestBody InspeccionRequest request) {
        return ResponseEntity.ok(articuloService.inspeccionar(principal.getName(), id, request));
    }
}
