package com.auctionsystem.duenio;

import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.auctionsystem.duenio.dto.DuenioProductoRequest;
import com.auctionsystem.duenio.dto.DuenioProductoResponse;
import com.auctionsystem.entities.Duenio;
import com.auctionsystem.entities.Empleado;
import com.auctionsystem.entities.Producto;
import com.auctionsystem.repositories.DuenioRepository;
import com.auctionsystem.repositories.EmpleadoRepository;
import com.auctionsystem.repositories.ProductoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DuenioProductoService {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final DuenioRepository duenioRepository;
    private final ProductoRepository productoRepository;
    private final EmpleadoRepository empleadoRepository;

    @Transactional
    public DuenioProductoResponse registrarProducto(String email, DuenioProductoRequest request) {
        Duenio duenio = getDuenioFromEmail(email);

        Empleado revisorPlaceholder = empleadoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay empleados registrados en el sistema"));

        Producto producto = Producto.builder()
                .fecha(LocalDate.now())
                .disponible("si")
                .titulo(request.titulo())
                .descripcionCatalogo(request.descripcionCatalogo() != null ? request.descripcionCatalogo() : "No Posee")
                .descripcionCompleta(request.descripcionCompleta())
                .historia(request.historia())
                .declaraPropiedad(request.declaraPropiedad())
                .origenLicit(request.origenLicit())
                .estadoInspeccion(Producto.ESTADO_PENDIENTE)
                .fechaInspeccion(LocalDateTime.now())
                .revisor(revisorPlaceholder)
                .duenio(duenio)
                .build();

        producto = productoRepository.save(producto);
        return toResponse(producto);
    }

    @Transactional(readOnly = true)
    public List<DuenioProductoResponse> listarProductos(String email) {
        Duenio duenio = getDuenioFromEmail(email);
        return productoRepository.findByDuenioIdOrderByIdDesc(duenio.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Duenio getDuenioFromEmail(String email) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (usuario.getPersonaId() == null) {
            throw new IllegalArgumentException("El usuario no tiene persona asociada");
        }

        return duenioRepository.findById(usuario.getPersonaId())
                .orElseThrow(() -> new IllegalArgumentException("El usuario no esta registrado como duenio"));
    }

    private DuenioProductoResponse toResponse(Producto p) {
        return new DuenioProductoResponse(
                p.getId(),
                p.getTitulo(),
                p.getDescripcionCatalogo(),
                p.getDescripcionCompleta(),
                p.getHistoria(),
                p.isDeclaraPropiedad(),
                p.isOrigenLicit(),
                p.getEstadoInspeccion(),
                p.getFecha()
        );
    }
}
