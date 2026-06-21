package com.auctionsystem.catalog;

import com.auctionsystem.catalog.dto.CatalogoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalogo publico de una subasta. El precio base de cada item solo se incluye
 * si el llamador esta autenticado (consigna).
 */
@RestController
@RequestMapping("/api/subastas")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/{subastaId}/catalogo")
    public ResponseEntity<CatalogoResponse> catalogo(@PathVariable Integer subastaId) {
        boolean autenticado = usuarioAutenticado();
        return ResponseEntity.ok(catalogoService.obtenerCatalogo(subastaId, autenticado));
    }

    private boolean usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && auth.getPrincipal() != null
                && !"anonymousUser".equals(auth.getPrincipal());
    }
}
