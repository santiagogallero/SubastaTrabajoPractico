package com.auctionsystem.product;

import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.auctionsystem.entities.Duenio;
import com.auctionsystem.entities.Empleado;
import com.auctionsystem.entities.Foto;
import com.auctionsystem.entities.Persona;
import com.auctionsystem.entities.Producto;
import com.auctionsystem.product.dto.ArticuloResponse;
import com.auctionsystem.product.dto.InspeccionRequest;
import com.auctionsystem.product.dto.PublicarArticuloRequest;
import com.auctionsystem.repositories.DuenioRepository;
import com.auctionsystem.repositories.EmpleadoRepository;
import com.auctionsystem.repositories.FotoRepository;
import com.auctionsystem.repositories.PersonaRepository;
import com.auctionsystem.repositories.ProductoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo de "vender un articulo propio" con inspeccion.
 *
 * <ol>
 *   <li>Un postor publica un articulo (titulo, categoria, descripcion,
 *       procedencia y fotos). Se crea el {@link Duenio} asociado a su persona si
 *       todavia no existe y el {@link Producto} queda en estado PENDIENTE.</li>
 *   <li>Un empleado revisa los articulos pendientes y los aprueba o rechaza.
 *       Al aprobar, el articulo queda disponible para catalogar y subastar.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticuloService {

    public static final int MIN_FOTOS = 6;

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final PersonaRepository personaRepository;
    private final DuenioRepository duenioRepository;
    private final ProductoRepository productoRepository;
    private final FotoRepository fotoRepository;
    private final EmpleadoRepository empleadoRepository;

    @Transactional
    public ArticuloResponse publicar(String email, PublicarArticuloRequest request) {
        if (request == null || isBlank(request.titulo())) {
            throw new IllegalArgumentException("El titulo del articulo es obligatorio");
        }
        if (isBlank(request.descripcionCompleta())) {
            throw new IllegalArgumentException("La descripcion completa es obligatoria");
        }
        if (!Boolean.TRUE.equals(request.declaraPropiedad())) {
            throw new IllegalArgumentException("Debe declarar ser dueno legitimo del articulo");
        }
        if (!Boolean.TRUE.equals(request.origenLicit())) {
            throw new IllegalArgumentException("Debe declarar que el articulo tiene origen licito");
        }
        long fotosValidas = contarFotosValidas(request.fotos());
        if (fotosValidas < MIN_FOTOS) {
            throw new IllegalArgumentException("Debe adjuntar al menos " + MIN_FOTOS + " fotos del articulo");
        }

        Duenio duenio = obtenerOCrearDuenio(email);

        Empleado empleadoPlaceholder = empleadoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay empleados registrados en el sistema"));

        Producto producto = Producto.builder()
                .fecha(LocalDate.now())
                .disponible("no")
                .titulo(request.titulo().trim())
                .categoria(request.categoria() != null ? request.categoria().trim() : null)
                .descripcionCatalogo(request.titulo().trim())
                .descripcionCompleta(request.descripcionCompleta().trim())
                .historia(request.historia() != null ? request.historia().trim() : null)
                .declaraPropiedad(true)
                .origenLicit(true)
                .estadoInspeccion(Producto.ESTADO_PENDIENTE)
                .revisor(empleadoPlaceholder)
                .duenio(duenio)
                .build();
        producto = productoRepository.save(producto);

        guardarFotos(producto, request.fotos());

        log.info("[ARTICULO] Publicado articulo {} por {} ({} fotos)", producto.getId(), email, fotosValidas);
        return ArticuloResponse.de(producto, fotosValidas);
    }

    @Transactional(readOnly = true)
    public List<ArticuloResponse> misArticulos(String email) {
        Integer personaId = personaIdDe(email);
        return productoRepository.findByDuenioIdOrderByIdDesc(personaId).stream()
                .map(p -> ArticuloResponse.de(p, fotoRepository.countByProductoId(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ArticuloResponse> pendientesDeInspeccion() {
        return productoRepository.findByEstadoInspeccionOrderByIdAsc(Producto.ESTADO_PENDIENTE).stream()
                .map(p -> ArticuloResponse.de(p, fotoRepository.countByProductoId(p.getId())))
                .toList();
    }

    @Transactional
    public ArticuloResponse inspeccionar(String email, Integer productoId, InspeccionRequest request) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Articulo no encontrado"));

        if (!Producto.ESTADO_PENDIENTE.equalsIgnoreCase(producto.getEstadoInspeccion())) {
            throw new IllegalArgumentException("El articulo ya fue inspeccionado");
        }

        if (!request.aprobado() && isBlank(request.motivo())) {
            throw new IllegalArgumentException("Debe indicar el motivo del rechazo");
        }

        producto.setRevisor(empleadoDe(email));
        producto.setFechaInspeccion(LocalDateTime.now());
        if (request.aprobado()) {
            producto.setEstadoInspeccion(Producto.ESTADO_APROBADO);
            producto.setDisponible("si");
            producto.setMotivoRechazo(null);
        } else {
            producto.setEstadoInspeccion(Producto.ESTADO_RECHAZADO);
            producto.setDisponible("no");
            producto.setMotivoRechazo(request.motivo().trim());
        }
        producto = productoRepository.save(producto);

        log.info("[ARTICULO] Articulo {} inspeccionado por {} -> {}", productoId, email, producto.getEstadoInspeccion());
        return ArticuloResponse.de(producto, fotoRepository.countByProductoId(producto.getId()));
    }

    private long contarFotosValidas(List<String> fotos) {
        if (fotos == null) {
            return 0;
        }
        return fotos.stream()
                .filter(f -> {
                    byte[] bytes = decodificarBase64(f);
                    return bytes != null && bytes.length > 0;
                })
                .count();
    }

    private Duenio obtenerOCrearDuenio(String email) {
        Persona persona = personaDe(email);
        return duenioRepository.findById(persona.getId())
                .orElseGet(() -> {
                    Empleado verificador = empleadoRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new IllegalStateException("No hay empleados registrados en el sistema"));
                    return duenioRepository.save(Duenio.builder()
                            .persona(persona)
                            .verificador(verificador)
                            .build());
                });
    }

    private void guardarFotos(Producto producto, List<String> fotos) {
        if (fotos == null) {
            return;
        }
        for (String foto : fotos) {
            byte[] bytes = decodificarBase64(foto);
            if (bytes == null || bytes.length == 0) {
                continue;
            }
            fotoRepository.save(Foto.builder()
                    .producto(producto)
                    .foto(bytes)
                    .build());
        }
    }

    private byte[] decodificarBase64(String data) {
        if (isBlank(data)) {
            return null;
        }
        String limpio = data;
        int coma = limpio.indexOf(',');
        if (limpio.startsWith("data:") && coma > 0) {
            limpio = limpio.substring(coma + 1);
        }
        try {
            return Base64.getDecoder().decode(limpio.trim());
        } catch (IllegalArgumentException e) {
            log.debug("Foto en base64 invalida, se omite", e);
            return null;
        }
    }

    private Empleado empleadoDe(String email) {
        Integer personaId = personaIdDe(email);
        return empleadoRepository.findById(personaId).orElse(null);
    }

    private Persona personaDe(String email) {
        Integer personaId = personaIdDe(email);
        return personaRepository.findById(personaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe persona asociada al usuario"));
    }

    private Integer personaIdDe(String email) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no vinculado"));
        if (usuario.getPersonaId() == null) {
            throw new IllegalArgumentException("El usuario no tiene una persona asociada");
        }
        return usuario.getPersonaId();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
