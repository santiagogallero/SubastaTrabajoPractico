package com.auctionsystem;

import com.auctionsystem.auction.SubastaConfigExt;
import com.auctionsystem.auction.SubastaConfigExtRepository;
import com.auctionsystem.entities.Catalogo;
import com.auctionsystem.entities.Duenio;
import com.auctionsystem.entities.Empleado;
import com.auctionsystem.entities.Persona;
import com.auctionsystem.entities.Producto;
import com.auctionsystem.entities.Sector;
import com.auctionsystem.entities.Seguro;
import com.auctionsystem.entities.Subasta;
import com.auctionsystem.entities.ItemCatalogo;
import com.auctionsystem.repositories.CatalogoRepository;
import com.auctionsystem.repositories.DuenioRepository;
import com.auctionsystem.repositories.EmpleadoRepository;
import com.auctionsystem.repositories.ItemCatalogoRepository;
import com.auctionsystem.repositories.PersonaRepository;
import com.auctionsystem.repositories.ProductoRepository;
import com.auctionsystem.repositories.SeguroRepository;
import com.auctionsystem.repositories.SectorRepository;
import com.auctionsystem.repositories.SubastaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder {

    private final SubastaRepository subastaRepository;
    private final SubastaConfigExtRepository subastaConfigExtRepository;
    private final PersonaRepository personaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final SectorRepository sectorRepository;
    private final DuenioRepository duenioRepository;
    private final ProductoRepository productoRepository;
    private final SeguroRepository seguroRepository;
    private final CatalogoRepository catalogoRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (subastaRepository.count() > 0) {
            return;
        }

        Empleado empleado = crearEmpleado();
        Duenio duenio = crearDuenio(empleado);
        Seguro seguro = crearSeguro();
        Producto producto = crearProducto(empleado, duenio, seguro);

        Subasta subastaComun = crearSubastaAbierta("COMUN", "Salon Principal - Buenos Aires", LocalTime.MIDNIGHT);
        configurarSubasta(subastaComun);
        Catalogo catalogo = crearCatalogo(subastaComun, empleado);
        crearItemCatalogo(catalogo, producto);

        crearSubastasInformativas();

        log.info("[DEV] Infraestructura sembrada: subasta ABIERTA id={} (COMUN), producto id={}",
                subastaComun.getId(), producto.getId());
        log.info("[DEV] Registra postores desde la app. Admin: admin@auction.com / Admin123!");
    }

    private Empleado crearEmpleado() {
        Sector sector = sectorRepository.save(Sector.builder()
                .nombreSector("Tasaciones")
                .codigoSector("TAS")
                .build());

        Persona persona = personaRepository.save(Persona.builder()
                .documento("20100100")
                .nombre("Empleado Tasador")
                .direccion("Av. Corrientes 1000, CABA")
                .estado("ACTIVO")
                .build());

        return empleadoRepository.save(Empleado.builder()
                .persona(persona)
                .cargo("Tasador")
                .sector(sector)
                .build());
    }

    private Duenio crearDuenio(Empleado verificador) {
        Persona persona = personaRepository.save(Persona.builder()
                .documento("27200200")
                .nombre("Maria Duenia")
                .direccion("Calle Falsa 123, CABA")
                .estado("ACTIVO")
                .build());

        return duenioRepository.save(Duenio.builder()
                .persona(persona)
                .verificacionFinanciera("si")
                .verificacionJudicial("si")
                .calificacionRiesgo(80)
                .verificador(verificador)
                .build());
    }

    private Seguro crearSeguro() {
        return seguroRepository.save(Seguro.builder()
                .nroPoliza("POL-DEV-001")
                .compania("Aseguradora Internacional SA")
                .polizaCombinada("no")
                .importe(new BigDecimal("250000.00"))
                .contactoTelefono("+54 11 5555-0100")
                .contactoEmail("contacto@aseguradora.example")
                .build());
    }

    private Producto crearProducto(Empleado revisor, Duenio duenio, Seguro seguro) {
        return productoRepository.save(Producto.builder()
                .fecha(LocalDate.now())
                .disponible("si")
                .titulo("Reloj de coleccion suizo")
                .categoria("Relojeria de Lujo")
                .descripcionCatalogo("Reloj de coleccion suizo, edicion limitada")
                .descripcionCompleta("Reloj automatico de coleccion, caja de acero, fabricado en 1968. Excelente estado.")
                .historia("Procedente de una coleccion privada europea, con caja y papeles originales.")
                .declaraPropiedad(true)
                .origenLicit(true)
                .estadoInspeccion(Producto.ESTADO_APROBADO)
                .fechaInspeccion(LocalDateTime.now())
                .revisor(revisor)
                .duenio(duenio)
                .seguro(seguro)
                .build());
    }

    private Subasta crearSubastaAbierta(String categoria, String ubicacion, LocalTime hora) {
        return subastaRepository.save(Subasta.builder()
                .fecha(LocalDate.now())
                .hora(hora)
                .estado("ABIERTA")
                .ubicacion(ubicacion)
                .capacidadAsistentes(150)
                .tieneDeposito("SI")
                .seguridadPropia("SI")
                .categoria(categoria)
                .streamingUrl("https://streaming.example/live/subasta-principal")
                .depositoNombre("Deposito Norte")
                .depositoDireccion("Av. del Libertador 5800, CABA")
                .build());
    }

    private void configurarSubasta(Subasta subasta) {
        SubastaConfigExt config = new SubastaConfigExt();
        config.setSubasta(subasta);
        config.setMoneda("ARS");
        config.setDuracionMinutos(60 * 24 * 30);
        subastaConfigExtRepository.save(config);
    }

    private Catalogo crearCatalogo(Subasta subasta, Empleado responsable) {
        return catalogoRepository.save(Catalogo.builder()
                .descripcion("Catalogo subasta de relojes y joyeria")
                .subasta(subasta)
                .responsable(responsable)
                .build());
    }

    private void crearItemCatalogo(Catalogo catalogo, Producto producto) {
        itemCatalogoRepository.save(ItemCatalogo.builder()
                .catalogo(catalogo)
                .producto(producto)
                .precioBase(new BigDecimal("10000.00"))
                .comision(new BigDecimal("1000.00"))
                .subastado("no")
                .build());
    }

    private void crearSubastasInformativas() {
        subastaRepository.saveAll(List.of(
                Subasta.builder()
                        .fecha(LocalDate.now().plusDays(5))
                        .hora(LocalTime.of(14, 30))
                        .estado("PENDIENTE")
                        .ubicacion("Sede Cordoba - Centro de Convenciones")
                        .capacidadAsistentes(80)
                        .tieneDeposito("SI")
                        .seguridadPropia("SI")
                        .categoria("Oro")
                        .build(),
                Subasta.builder()
                        .fecha(LocalDate.now().plusDays(9))
                        .hora(LocalTime.of(11, 0))
                        .estado("PENDIENTE")
                        .ubicacion("Palacio San Martin - Buenos Aires")
                        .capacidadAsistentes(200)
                        .tieneDeposito("SI")
                        .seguridadPropia("SI")
                        .categoria("Platino")
                        .build()
        ));
    }
}
