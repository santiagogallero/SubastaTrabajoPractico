package com.auctionsystem.auth;

import com.auctionsystem.auction.SubastaConfigExt;
import com.auctionsystem.auction.SubastaConfigExtRepository;
import com.auctionsystem.entities.Catalogo;
import com.auctionsystem.entities.Cliente;
import com.auctionsystem.entities.Duenio;
import com.auctionsystem.entities.Empleado;
import com.auctionsystem.entities.ItemCatalogo;
import com.auctionsystem.entities.Persona;
import com.auctionsystem.entities.Producto;
import com.auctionsystem.entities.Subasta;
import com.auctionsystem.repositories.CatalogoRepository;
import com.auctionsystem.repositories.ClienteRepository;
import com.auctionsystem.repositories.DuenioRepository;
import com.auctionsystem.repositories.EmpleadoRepository;
import com.auctionsystem.repositories.ItemCatalogoRepository;
import com.auctionsystem.repositories.PersonaRepository;
import com.auctionsystem.repositories.ProductoRepository;
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
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevSantiagoSeeder {

    private static final String PASSWORD = "Test1234!";

    // email | documento | nombre | categoriaCliente | esDuenio
    private static final List<String[]> DEV_USERS = List.of(
            new String[]{"santiago1gallero@gmail.com", "30999888", "Santiago Gallero", "PLATINO", "true"},
            new String[]{"jiloji8868@dyleris.com",     "30999777", "Jiloji Dev",       "COMUN",   "false"},
            new String[]{"dueniotest@auction.com",     "30999555", "Duenio Test",      "COMUN",   "true"}
    );

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final RolRepository rolRepository;
    private final PersonaRepository personaRepository;
    private final ClienteRepository clienteRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final DuenioRepository duenioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final SubastaRepository subastaRepository;
    private final SubastaConfigExtRepository subastaConfigExtRepository;
    private final CatalogoRepository catalogoRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final ProductoRepository productoRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    @Transactional
    public void run() {
        Empleado empleado = empleadoRepository.findAll().stream().findFirst().orElse(null);
        if (empleado == null) {
            log.warn("[DEV] Sin empleado en DB — los Duenios se crearan en el proximo reinicio");
        }

        Rol rolPostor = rolRepository.findByNombre("POSTOR")
                .orElseThrow(() -> new IllegalStateException("Rol POSTOR no encontrado"));
        Rol rolDuenio = rolRepository.findByNombre("DUENIO")
                .orElseThrow(() -> new IllegalStateException("Rol DUENIO no encontrado"));

        for (String[] u : DEV_USERS) {
            boolean esDuenio = Boolean.parseBoolean(u[4]);
            seedUsuario(u[0], u[1], u[2], u[3], rolPostor, esDuenio ? rolDuenio : null, empleado);
        }

        seedSubastasExtra(empleado);
    }

    private void seedUsuario(String email, String documento, String nombre, String categoria,
                              Rol rolPostor, Rol rolDuenio, Empleado empleado) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email).orElseGet(() -> {
            Persona persona = personaRepository.save(Persona.builder()
                    .documento(documento)
                    .nombre(nombre)
                    .direccion("Buenos Aires, Argentina")
                    .estado("ACTIVO")
                    .build());

            UsuarioAuth nuevo = new UsuarioAuth();
            nuevo.setEmail(email);
            nuevo.setPasswordHash(passwordEncoder.encode(PASSWORD));
            nuevo.setEstado("ACTIVO");
            nuevo.setPersonaId(persona.getId());
            nuevo.setCreatedAt(LocalDateTime.now());
            nuevo.setUpdatedAt(LocalDateTime.now());
            nuevo.getRoles().add(rolPostor);
            nuevo = usuarioAuthRepository.save(nuevo);

            clienteRepository.findByPersonaId(persona.getId()).orElseGet(() ->
                    clienteRepository.save(Cliente.builder()
                            .persona(persona)
                            .admitido("si")
                            .categoria(categoria)
                            .verificador(empleado)
                            .build()));

            log.info("[DEV] Usuario creado: {} / {} (categoria: {})", email, PASSWORD, categoria);
            return nuevo;
        });

        // Agregar rol DUENIO si corresponde y no lo tiene
        if (rolDuenio != null) {
            boolean yaTieneDuenio = usuario.getRoles().stream()
                    .anyMatch(r -> "DUENIO".equalsIgnoreCase(r.getNombre()));
            if (!yaTieneDuenio) {
                usuario.getRoles().add(rolDuenio);
                usuarioAuthRepository.save(usuario);
                log.info("[DEV] Rol DUENIO agregado a {}", email);
            }

            // Crear entidad Duenio si no existe y hay un empleado disponible
            if (usuario.getPersonaId() != null && empleado != null) {
                duenioRepository.findById(usuario.getPersonaId()).orElseGet(() -> {
                    Persona persona = personaRepository.findById(usuario.getPersonaId()).orElseThrow();
                    Duenio duenio = duenioRepository.save(Duenio.builder()
                            .persona(persona)
                            .verificacionFinanciera("si")
                            .verificacionJudicial("si")
                            .calificacionRiesgo(5)
                            .verificador(empleado)
                            .build());
                    log.info("[DEV] Entidad Duenio creada para {}", email);
                    return duenio;
                });
            }
        }

        // Actualizar categoría si cambió
        if (usuario.getPersonaId() != null) {
            clienteRepository.findByPersonaId(usuario.getPersonaId()).ifPresent(cliente -> {
                if (!categoria.equalsIgnoreCase(cliente.getCategoria())) {
                    cliente.setCategoria(categoria);
                    clienteRepository.save(cliente);
                    log.info("[DEV] Categoria actualizada a {} para {}", categoria, email);
                }
            });
        }

        boolean tieneMedioVerificado = medioPagoRepository
                .existsByUsuarioIdAndVerificadoTrueAndActivoTrue(usuario.getId());
        if (!tieneMedioVerificado) {
            MedioPago medio = new MedioPago();
            medio.setUsuario(usuario);
            medio.setTipo("TRANSFERENCIA");
            medio.setAliasDescripcion("Cuenta banco dev");
            medio.setMoneda("ARS");
            medio.setMontoGarantia(new BigDecimal("1000000.00"));
            medio.setVerificado(true);
            medio.setActivo(true);
            medioPagoRepository.save(medio);
            log.info("[DEV] Medio de pago verificado agregado a {}", email);
        }
    }

    private void seedSubastasExtra(Empleado empleado) {
        List<Subasta> existentes = subastaRepository.findAll();

        boolean tieneOroAbierta = existentes.stream()
                .anyMatch(s -> "ORO".equalsIgnoreCase(s.getCategoria()) && "ABIERTA".equalsIgnoreCase(s.getEstado()));
        boolean tienePlatinoAbierta = existentes.stream()
                .anyMatch(s -> "PLATINO".equalsIgnoreCase(s.getCategoria()) && "ABIERTA".equalsIgnoreCase(s.getEstado()));

        Duenio duenio = duenioRepository.findAll().stream().findFirst().orElse(null);

        if (empleado == null || duenio == null) {
            return;
        }

        if (!tieneOroAbierta) {
            crearSubastaConItem("ORO", "Sede Palermo - Buenos Aires", duenio, empleado,
                    "Collar de oro 18k", "Collar artesanal de oro 18 quilates con rubies naturales.",
                    new BigDecimal("50000.00"));
            log.info("[DEV] Subasta ABIERTA ORO creada");
        }

        if (!tienePlatinoAbierta) {
            crearSubastaConItem("PLATINO", "Salon VIP - Puerto Madero", duenio, empleado,
                    "Diamante talla brillante 2ct", "Diamante certificado GIA, talla brillante, 2 quilates, color D, pureza VVS1.",
                    new BigDecimal("200000.00"));
            log.info("[DEV] Subasta ABIERTA PLATINO creada");
        }
    }

    private void crearSubastaConItem(String categoria, String ubicacion, Duenio duenio, Empleado empleado,
                                      String tituloProducto, String descripcion, BigDecimal precioBase) {
        Subasta subasta = subastaRepository.save(Subasta.builder()
                .fecha(LocalDate.now())
                .hora(LocalTime.MIDNIGHT)
                .estado("ABIERTA")
                .ubicacion(ubicacion)
                .capacidadAsistentes(50)
                .tieneDeposito("SI")
                .seguridadPropia("SI")
                .categoria(categoria)
                .build());

        SubastaConfigExt config = new SubastaConfigExt();
        config.setSubasta(subasta);
        config.setMoneda("ARS");
        config.setDuracionMinutos(60 * 24 * 30);
        subastaConfigExtRepository.save(config);

        Producto producto = productoRepository.save(Producto.builder()
                .fecha(LocalDate.now())
                .disponible("si")
                .titulo(tituloProducto)
                .categoria(categoria)
                .descripcionCatalogo(descripcion)
                .descripcionCompleta(descripcion)
                .declaraPropiedad(true)
                .origenLicit(true)
                .estadoInspeccion(Producto.ESTADO_APROBADO)
                .fechaInspeccion(LocalDateTime.now())
                .revisor(empleado)
                .duenio(duenio)
                .build());

        Catalogo catalogo = catalogoRepository.save(Catalogo.builder()
                .descripcion("Catalogo subasta " + categoria.toLowerCase())
                .subasta(subasta)
                .responsable(empleado)
                .build());

        itemCatalogoRepository.save(ItemCatalogo.builder()
                .catalogo(catalogo)
                .producto(producto)
                .precioBase(precioBase)
                .comision(precioBase.multiply(new BigDecimal("0.10")))
                .subastado("no")
                .build());
    }
}
