package com.auctionsystem;

import com.auctionsystem.entities.Subasta;
import com.auctionsystem.repositories.SubastaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder {

    private final SubastaRepository subastaRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seedSubastas() {
        if (subastaRepository.count() > 0) return;

        subastaRepository.saveAll(List.of(
            Subasta.builder()
                .fecha(LocalDate.of(2026, 6, 10))
                .hora(LocalTime.of(10, 0))
                .estado("ACTIVA")
                .ubicacion("Salón Principal - Buenos Aires")
                .capacidadAsistentes(150)
                .tieneDeposito("SI")
                .seguridadPropia("NO")
                .categoria("Oro")
                .build(),
            Subasta.builder()
                .fecha(LocalDate.of(2026, 6, 12))
                .hora(LocalTime.of(14, 30))
                .estado("ACTIVA")
                .ubicacion("Sede Córdoba - Centro de Convenciones")
                .capacidadAsistentes(80)
                .tieneDeposito("SI")
                .seguridadPropia("SI")
                .categoria("Plata")
                .build(),
            Subasta.builder()
                .fecha(LocalDate.of(2026, 6, 20))
                .hora(LocalTime.of(11, 0))
                .estado("PENDIENTE")
                .ubicacion("Palacio San Martín - Buenos Aires")
                .capacidadAsistentes(200)
                .tieneDeposito("SI")
                .seguridadPropia("SI")
                .categoria("Platino")
                .build(),
            Subasta.builder()
                .fecha(LocalDate.of(2026, 6, 25))
                .hora(LocalTime.of(9, 0))
                .estado("PENDIENTE")
                .ubicacion("Salón de Exposiciones - Mendoza")
                .capacidadAsistentes(50)
                .tieneDeposito("SI")
                .seguridadPropia("SI")
                .categoria("Común")
                .build(),
            Subasta.builder()
                .fecha(LocalDate.of(2026, 6, 28))
                .hora(LocalTime.of(16, 0))
                .estado("PENDIENTE")
                .ubicacion("Centro Cultural Rosario")
                .capacidadAsistentes(120)
                .tieneDeposito("SI")
                .seguridadPropia("NO")
                .categoria("Plata")
                .build()
        ));
    }
}
