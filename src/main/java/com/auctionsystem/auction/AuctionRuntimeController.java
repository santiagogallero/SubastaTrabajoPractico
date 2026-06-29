package com.auctionsystem.auction;

import com.auctionsystem.auction.dto.CierreSubastaResponse;
import com.auctionsystem.auction.dto.ConfigurarMonedaSubastaRequest;
import com.auctionsystem.auction.dto.ConfigurarDuracionSubastaRequest;
import com.auctionsystem.auction.dto.CalcularComisionRequest;
import com.auctionsystem.auction.dto.ComisionResponse;
import com.auctionsystem.auction.dto.ConectarSubastaRequest;
import com.auctionsystem.auction.dto.PujaHistorialItem;
import com.auctionsystem.auction.dto.PujaResponse;
import com.auctionsystem.auction.dto.PujarRequest;
import com.auctionsystem.auction.dto.StreamingResponse;
import com.auctionsystem.auction.dto.SubastaTimingResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auction-runtime")
@RequiredArgsConstructor
public class AuctionRuntimeController {

    private final AuctionRuntimeService auctionRuntimeService;
    private final ComisionService comisionService;

    @PostMapping("/subasta/configurar-moneda")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<String> configurarMoneda(@Valid @RequestBody ConfigurarMonedaSubastaRequest request) {
        auctionRuntimeService.configurarMoneda(request.subastaId(), request.moneda());
        return ResponseEntity.ok("Moneda de subasta configurada");
    }

    @PostMapping("/subasta/configurar-duracion")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<String> configurarDuracion(@Valid @RequestBody ConfigurarDuracionSubastaRequest request) {
        auctionRuntimeService.configurarDuracion(request.subastaId(), request.duracionMinutos());
        return ResponseEntity.ok("Duracion de subasta configurada");
    }

    @GetMapping("/subasta/{subastaId}/timing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubastaTimingResponse> obtenerTiming(@PathVariable Integer subastaId) {
        return ResponseEntity.ok(auctionRuntimeService.obtenerTimingSubasta(subastaId));
    }

    @GetMapping("/subasta/{subastaId}/streaming")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StreamingResponse> obtenerStreaming(@PathVariable Integer subastaId) {
        return ResponseEntity.ok(auctionRuntimeService.obtenerStreaming(subastaId));
    }

    @PostMapping("/subasta/conectar")
    @PreAuthorize("hasRole('POSTOR')")
    public ResponseEntity<String> conectar(@Valid @RequestBody ConectarSubastaRequest request, Principal principal) {
        auctionRuntimeService.conectarASubasta(principal.getName(), request.subastaId());
        return ResponseEntity.ok("Conexion a subasta realizada");
    }

    @DeleteMapping("/subasta/desconectar")
    @PreAuthorize("hasRole('POSTOR')")
    public ResponseEntity<String> desconectar(Principal principal) {
        auctionRuntimeService.desconectarDeSubasta(principal.getName());
        return ResponseEntity.ok("Desconexion de subasta realizada");
    }

    @PostMapping("/pujas")
    @PreAuthorize("hasRole('POSTOR')")
    public ResponseEntity<PujaResponse> pujar(@Valid @RequestBody PujarRequest request, Principal principal) {
        return ResponseEntity.ok(auctionRuntimeService.pujar(principal.getName(), request));
    }

    @GetMapping("/pujas/historial/{itemId}")
    @PreAuthorize("hasAnyRole('POSTOR','ADMIN','EMPLEADO','SUBASTADOR')")
    public ResponseEntity<List<PujaHistorialItem>> historial(@PathVariable Integer itemId) {
        return ResponseEntity.ok(auctionRuntimeService.historial(itemId));
    }

    @PostMapping("/subasta/{subastaId}/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<String> iniciarSubasta(
            @PathVariable Integer subastaId,
            @RequestBody(required = false) IniciarSubastaRequest request) {
        Integer duracion = request != null ? request.duracionItemMinutos() : null;
        auctionRuntimeService.iniciarSubastaRuntime(subastaId, duracion);
        return ResponseEntity.ok("Subasta iniciada");
    }

    public record IniciarSubastaRequest(Integer duracionItemMinutos) {}

    @PostMapping("/subasta/{subastaId}/cerrar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<CierreSubastaResponse> cerrarSubasta(@PathVariable Integer subastaId) {
        return ResponseEntity.ok(auctionRuntimeService.cerrarSubasta(subastaId));
    }

    @PostMapping("/comisiones/calcular")
    @PreAuthorize("hasAnyRole('POSTOR','DUENIO','ADMIN','EMPLEADO','SUBASTADOR')")
    public ResponseEntity<ComisionResponse> calcularComision(@Valid @RequestBody CalcularComisionRequest request) {
        return ResponseEntity.ok(comisionService.calcular(request.importeFinal()));
    }
}
