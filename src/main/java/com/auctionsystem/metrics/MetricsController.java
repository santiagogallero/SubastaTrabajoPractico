package com.auctionsystem.metrics;

import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('POSTOR','ADMIN','EMPLEADO')")
    public ResponseEntity<MetricasUsuarioResponse> misMetricas(Principal principal) {
        return ResponseEntity.ok(metricsService.metricasDe(principal.getName()));
    }
}
