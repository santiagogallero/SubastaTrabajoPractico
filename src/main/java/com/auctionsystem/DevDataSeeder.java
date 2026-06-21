package com.auctionsystem;

import com.auctionsystem.auction.SubastaConfigExt;
import com.auctionsystem.auction.SubastaConfigExtRepository;
import com.auctionsystem.auth.MedioPago;
import com.auctionsystem.auth.MedioPagoRepository;
import com.auctionsystem.auth.RegistroPostor;
import com.auctionsystem.auth.RegistroPostorRepository;
import com.auctionsystem.auth.Rol;
import com.auctionsystem.auth.RolRepository;
import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.auctionsystem.entities.Catalogo;
import com.auctionsystem.entities.Cliente;
import com.auctionsystem.entities.Coleccion;
import com.auctionsystem.entities.Duenio;
import com.auctionsystem.entities.Empleado;
import com.auctionsystem.entities.ItemCatalogo;
import com.auctionsystem.entities.Persona;
import com.auctionsystem.entities.Producto;
import com.auctionsystem.entities.Sector;
import com.auctionsystem.entities.Seguro;
import com.auctionsystem.entities.Subasta;
import com.auctionsystem.repositories.CatalogoRepository;
import com.auctionsystem.repositories.ClienteRepository;
import com.auctionsystem.repositories.ColeccionRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sembrado de datos para desarrollo.
 *
 * <p>Arma un circuito de puja completo y funcional: empleado/tasador, dueño,
 * producto, subasta ABIERTA con su catalogo e item, y un postor ya aprobado con
 * medio de pago verificado para que se pueda iniciar sesion, conectarse y pujar
 * de punta a punta contra el backend real.</p>
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder {

    private static final String POSTOR_EMAIL = "postor@auction.com";
    private static final String POSTOR_PASSWORD = "Postor123!";
    private static final String EMPLEADO_EMAIL = "empleado@auction.com";
    private static final String EMPLEADO_PASSWORD = "Empleado123!";

    private final SubastaRepository subastaRepository;
    private final SubastaConfigExtRepository subastaConfigExtRepository;
    private final PersonaRepository personaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final SectorRepository sectorRepository;
    private final DuenioRepository duenioRepository;
    private final ProductoRepository productoRepository;
    private final SeguroRepository seguroRepository;
    private final CatalogoRepository catalogoRepository;
    private final ColeccionRepository coleccionRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final RolRepository rolRepository;
    private final RegistroPostorRepository registroPostorRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (subastaRepository.count() > 0) {
            return;
        }

        Empleado empleado = crearEmpleado();
        crearUsuarioEmpleado(empleado);
        Duenio duenio = crearDuenio(empleado);
        Seguro seguro = crearSeguro();
        Producto producto = crearProducto(empleado, duenio, seguro);

        Subasta subasta = crearSubastaAbierta();
        configurarSubasta(subasta);
        Catalogo catalogo = crearCatalogo(subasta, empleado);
        crearItemCatalogo(catalogo, producto);

        crearSubastasInformativas();

        Persona postorPersona = crearPostorAprobado();
        crearArticuloPendiente(postorPersona);
        Producto productoPostor1 = null;
        Producto productoPostor2 = null;
        if (postorPersona != null) {
            productoPostor1 = crearArticuloAseguradoDelPostor(postorPersona, empleado);
            productoPostor2 = crearSegundoArticuloAprobadoDelPostor(postorPersona, empleado);
            crearSubastaColeccion(postorPersona, empleado, productoPostor1, productoPostor2);
        }

        log.info("[DEV] Circuito de puja sembrado. Postor de prueba: {} / {}", POSTOR_EMAIL, POSTOR_PASSWORD);
        log.info("[DEV] Empleado inspector de prueba: {} / {}", EMPLEADO_EMAIL, EMPLEADO_PASSWORD);
    }

    /** Usuario con rol EMPLEADO vinculado al tasador, para probar la inspeccion. */
    private void crearUsuarioEmpleado(Empleado empleado) {
        String email = EMPLEADO_EMAIL.toLowerCase(Locale.ROOT);
        if (usuarioAuthRepository.existsByEmail(email)) {
            return;
        }
        Rol rolEmpleado = rolRepository.findByNombre("EMPLEADO")
                .orElseThrow(() -> new IllegalStateException("Rol EMPLEADO no encontrado"));

        UsuarioAuth usuario = new UsuarioAuth();
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(EMPLEADO_PASSWORD));
        usuario.setEstado("ACTIVO");
        usuario.setPersonaId(empleado.getId());
        usuario.setCreatedAt(LocalDateTime.now());
        usuario.setUpdatedAt(LocalDateTime.now());
        usuario.getRoles().add(rolEmpleado);
        usuarioAuthRepository.save(usuario);
    }

    /** Articulo del postor a la espera de inspeccion, para poblar la cola del empleado. */
    private void crearArticuloPendiente(Persona postorPersona) {
        if (postorPersona == null) {
            return;
        }
        Duenio duenioPostor = duenioRepository.findById(postorPersona.getId())
                .orElseGet(() -> duenioRepository.save(Duenio.builder()
                        .persona(postorPersona)
                        .build()));

        productoRepository.save(Producto.builder()
                .fecha(LocalDate.now())
                .disponible("no")
                .titulo("Pintura al oleo - Paisaje patagonico")
                .categoria("Arte")
                .descripcionCatalogo("Pintura al oleo - Paisaje patagonico")
                .descripcionCompleta("Oleo sobre lienzo, 80x60cm, firmado al dorso. Heredado de un coleccionista privado.")
                .historia("Adquirido en una galeria de Bariloche en 1995. Sin restauraciones. Incluye certificado de autenticidad.")
                .declaraPropiedad(true)
                .origenLicit(true)
                .estadoInspeccion(Producto.ESTADO_PENDIENTE)
                .duenio(duenioPostor)
                .build());
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

    private Producto crearArticuloAseguradoDelPostor(Persona postor, Empleado revisor) {
        Duenio duenioPostor = duenioRepository.findById(postor.getId())
                .orElseGet(() -> duenioRepository.save(Duenio.builder()
                        .persona(postor)
                        .verificacionFinanciera("si")
                        .verificacionJudicial("si")
                        .calificacionRiesgo(90)
                        .verificador(revisor)
                        .build()));

        Seguro poliza = seguroRepository.save(Seguro.builder()
                .nroPoliza("POL-POSTOR-001")
                .compania("Mercantil Andina SA")
                .polizaCombinada("no")
                .importe(new BigDecimal("85000.00"))
                .contactoTelefono("+54 11 4321-9000")
                .contactoEmail("siniestros@mercantil.example")
                .build());

        return productoRepository.save(Producto.builder()
                .fecha(LocalDate.now())
                .disponible("si")
                .titulo("Anillo de oro con esmeralda")
                .categoria("Joyeria")
                .descripcionCatalogo("Anillo de oro con esmeralda")
                .descripcionCompleta("Anillo de oro 18K con esmeralda colombiana certificada, talla 14.")
                .historia("Adquirido en subasta privada en 2019, con certificado gemologico.")
                .declaraPropiedad(true)
                .origenLicit(true)
                .estadoInspeccion(Producto.ESTADO_APROBADO)
                .fechaInspeccion(LocalDateTime.now())
                .revisor(revisor)
                .duenio(duenioPostor)
                .seguro(poliza)
                .build());
    }

    private Producto crearSegundoArticuloAprobadoDelPostor(Persona postor, Empleado revisor) {
        Duenio duenioPostor = duenioRepository.findById(postor.getId())
                .orElseThrow(() -> new IllegalStateException("Duenio del postor no encontrado"));

        return productoRepository.save(Producto.builder()
                .fecha(LocalDate.now())
                .disponible("si")
                .titulo("Collar de perlas cultivadas")
                .categoria("Joyeria")
                .descripcionCatalogo("Collar de perlas cultivadas")
                .descripcionCompleta("Collar de 45 cm con perlas Akoya de 8 mm, cierre de oro blanco.")
                .historia("Pieza familiar transmitida por generaciones, con tasacion reciente.")
                .declaraPropiedad(true)
                .origenLicit(true)
                .estadoInspeccion(Producto.ESTADO_APROBADO)
                .fechaInspeccion(LocalDateTime.now())
                .revisor(revisor)
                .duenio(duenioPostor)
                .build());
    }

    private void crearSubastaColeccion(Persona postor, Empleado empleado, Producto p1, Producto p2) {
        Duenio duenioPostor = duenioRepository.findById(postor.getId()).orElseThrow();

        Subasta subastaColeccion = subastaRepository.save(Subasta.builder()
                .fecha(LocalDate.now())
                .hora(LocalTime.of(18, 0))
                .estado("ABIERTA")
                .ubicacion("Salon Colecciones - Palermo")
                .capacidadAsistentes(80)
                .tieneDeposito("SI")
                .seguridadPropia("SI")
                .categoria("Platino")
                .streamingUrl("https://streaming.example/live/coleccion-postor")
                .depositoNombre("Deposito Central")
                .depositoDireccion("Av. Industria 3200, CABA")
                .build());

        SubastaConfigExt config = new SubastaConfigExt();
        config.setSubasta(subastaColeccion);
        config.setMoneda("ARS");
        config.setDuracionMinutos(60 * 24 * 30);
        subastaConfigExtRepository.save(config);

        Catalogo catalogo = catalogoRepository.save(Catalogo.builder()
                .descripcion("Coleccion Juan Postor")
                .subasta(subastaColeccion)
                .responsable(empleado)
                .build());

        itemCatalogoRepository.save(ItemCatalogo.builder()
                .catalogo(catalogo).producto(p1)
                .precioBase(new BigDecimal("75000.00")).comision(BigDecimal.ZERO).subastado("no").build());
        itemCatalogoRepository.save(ItemCatalogo.builder()
                .catalogo(catalogo).producto(p2)
                .precioBase(new BigDecimal("42000.00")).comision(BigDecimal.ZERO).subastado("no").build());

        Coleccion coleccion = Coleccion.builder()
                .nombre("Coleccion " + postor.getNombre())
                .duenio(duenioPostor)
                .subasta(subastaColeccion)
                .fechaCreacion(LocalDateTime.now())
                .productos(new HashSet<>(List.of(p1, p2)))
                .build();
        coleccionRepository.save(coleccion);
    }

    private Subasta crearSubastaAbierta() {
        return subastaRepository.save(Subasta.builder()
                .fecha(LocalDate.now())
                .hora(LocalTime.MIDNIGHT)
                .estado("ABIERTA")
                .ubicacion("Salon Principal - Buenos Aires")
                .capacidadAsistentes(150)
                .tieneDeposito("SI")
                .seguridadPropia("SI")
                .categoria("Plata")
                .streamingUrl("https://streaming.example/live/subasta-principal")
                .depositoNombre("Deposito Norte")
                .depositoDireccion("Av. del Libertador 5800, CABA")
                .build());
    }

    private void configurarSubasta(Subasta subasta) {
        SubastaConfigExt config = new SubastaConfigExt();
        config.setSubasta(subasta);
        config.setMoneda("ARS");
        // Ventana amplia para que la subasta de demo siga abierta durante el desarrollo.
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

    private Persona crearPostorAprobado() {
        String email = POSTOR_EMAIL.toLowerCase(Locale.ROOT);
        if (usuarioAuthRepository.existsByEmail(email)) {
            return usuarioAuthRepository.findByEmail(email)
                    .map(UsuarioAuth::getPersonaId)
                    .flatMap(personaRepository::findById)
                    .orElse(null);
        }

        Rol rolPostor = rolRepository.findByNombre("POSTOR")
                .orElseThrow(() -> new IllegalStateException("Rol POSTOR no encontrado"));

        Persona persona = personaRepository.save(Persona.builder()
                .documento("30111222")
                .nombre("Juan Postor")
                .direccion("Av. Siempre Viva 742, CABA")
                .estado("ACTIVO")
                .build());

        UsuarioAuth usuario = new UsuarioAuth();
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(POSTOR_PASSWORD));
        usuario.setEstado("ACTIVO");
        usuario.setPersonaId(persona.getId());
        usuario.setCreatedAt(LocalDateTime.now());
        usuario.setUpdatedAt(LocalDateTime.now());
        usuario.getRoles().add(rolPostor);
        usuario = usuarioAuthRepository.save(usuario);

        RegistroPostor registro = new RegistroPostor();
        registro.setUsuario(usuario);
        registro.setEtapa("APROBADO");
        registro.setCategoria("PLATINO");
        registro.setDomicilioLegal("Av. Siempre Viva 742, CABA");
        registro.setPaisOrigen("Argentina");
        registro.setNumeroTramite("00123456789");
        registro.setVerificadoAt(LocalDateTime.now());
        registroPostorRepository.save(registro);

        clienteRepository.save(Cliente.builder()
                .persona(persona)
                .categoria("PLATINO")
                .admitido("si")
                .build());

        MedioPago medioPago = new MedioPago();
        medioPago.setUsuario(usuario);
        medioPago.setTipo("TARJETA_CREDITO");
        medioPago.setAliasDescripcion("Visa **** 4242");
        medioPago.setMoneda("ARS");
        medioPago.setVerificado(true);
        medioPago.setActivo(true);
        medioPagoRepository.save(medioPago);

        return persona;
    }
}
