package com.auctionsystem.coleccion;

import com.auctionsystem.coleccion.dto.AgregarProductoColeccionRequest;
import com.auctionsystem.coleccion.dto.ColeccionResponse;
import com.auctionsystem.coleccion.dto.ColeccionResumenResponse;
import com.auctionsystem.coleccion.dto.CrearColeccionRequest;
import com.auctionsystem.coleccion.dto.ProductoColeccionRequest;
import com.auctionsystem.entities.Catalogo;
import com.auctionsystem.entities.Coleccion;
import com.auctionsystem.entities.Duenio;
import com.auctionsystem.entities.Empleado;
import com.auctionsystem.entities.ItemCatalogo;
import com.auctionsystem.entities.Producto;
import com.auctionsystem.entities.Subasta;
import com.auctionsystem.repositories.CatalogoRepository;
import com.auctionsystem.repositories.ColeccionRepository;
import com.auctionsystem.repositories.DuenioRepository;
import com.auctionsystem.repositories.EmpleadoRepository;
import com.auctionsystem.repositories.ItemCatalogoRepository;
import com.auctionsystem.repositories.ProductoRepository;
import com.auctionsystem.repositories.SubastaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agrupa piezas aprobadas de un dueno en una subasta dedicada (consigna §11).
 */
@Service
@RequiredArgsConstructor
public class ColeccionService {

    private static final BigDecimal PRECIO_BASE_DEFAULT = new BigDecimal("50000");

    private final ColeccionRepository coleccionRepository;
    private final DuenioRepository duenioRepository;
    private final SubastaRepository subastaRepository;
    private final ProductoRepository productoRepository;
    private final CatalogoRepository catalogoRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final EmpleadoRepository empleadoRepository;

    @Transactional
    public ColeccionResponse crear(CrearColeccionRequest request) {
        if (request == null || isBlank(request.nombre())) {
            throw new IllegalArgumentException("El nombre de la coleccion es obligatorio");
        }
        if (request.duenioId() == null || request.subastaId() == null) {
            throw new IllegalArgumentException("Duenio y subasta son obligatorios");
        }
        if (request.productos() == null || request.productos().isEmpty()) {
            throw new IllegalArgumentException("Debe incluir al menos un producto aprobado");
        }
        if (coleccionRepository.findBySubastaId(request.subastaId()).isPresent()) {
            throw new IllegalArgumentException("La subasta ya tiene una coleccion asociada");
        }

        Duenio duenio = duenioRepository.findById(request.duenioId())
                .orElseThrow(() -> new IllegalArgumentException("Duenio no encontrado"));
        Subasta subasta = subastaRepository.findById(request.subastaId())
                .orElseThrow(() -> new IllegalArgumentException("Subasta no encontrada"));

        Coleccion coleccion = Coleccion.builder()
                .nombre(request.nombre().trim())
                .duenio(duenio)
                .subasta(subasta)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Catalogo catalogo = obtenerOCrearCatalogo(subasta, request.nombre().trim());

        for (ProductoColeccionRequest item : request.productos()) {
            Producto producto = validarProductoParaColeccion(item.productoId(), duenio);
            coleccion.getProductos().add(producto);
            agregarAlCatalogo(catalogo, producto, precioBaseDe(item));
        }

        coleccion = coleccionRepository.save(coleccion);
        return toResponse(coleccion);
    }

    @Transactional(readOnly = true)
    public ColeccionResponse obtener(Integer id) {
        Coleccion coleccion = coleccionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coleccion no encontrada"));
        return toResponse(coleccion);
    }

    @Transactional(readOnly = true)
    public ColeccionResponse obtenerPorSubasta(Integer subastaId) {
        Coleccion coleccion = coleccionRepository.findBySubastaId(subastaId)
                .orElseThrow(() -> new IllegalArgumentException("No hay coleccion para esta subasta"));
        return toResponse(coleccion);
    }

    @Transactional(readOnly = true)
    public List<ColeccionResumenResponse> listarResumen() {
        return coleccionRepository.findAllByOrderByIdAsc().stream()
                .map(this::toResumen)
                .toList();
    }

    @Transactional
    public ColeccionResponse agregarProducto(Integer coleccionId, AgregarProductoColeccionRequest request) {
        Coleccion coleccion = coleccionRepository.findById(coleccionId)
                .orElseThrow(() -> new IllegalArgumentException("Coleccion no encontrada"));
        if (request == null || request.productoId() == null) {
            throw new IllegalArgumentException("productoId es obligatorio");
        }

        Producto producto = validarProductoParaColeccion(request.productoId(), coleccion.getDuenio());
        if (coleccion.getProductos().stream().anyMatch(p -> p.getId().equals(producto.getId()))) {
            throw new IllegalArgumentException("El producto ya pertenece a la coleccion");
        }

        coleccion.getProductos().add(producto);
        Catalogo catalogo = catalogoRepository.findFirstBySubasta_Id(coleccion.getSubasta().getId())
                .orElseThrow(() -> new IllegalArgumentException("La subasta no tiene catalogo"));
        agregarAlCatalogo(catalogo, producto, precioBaseDe(request.productoId(), request.precioBase()));

        coleccion = coleccionRepository.save(coleccion);
        return toResponse(coleccion);
    }

    @Transactional
    public ColeccionResponse quitarProducto(Integer coleccionId, Integer productoId) {
        Coleccion coleccion = coleccionRepository.findById(coleccionId)
                .orElseThrow(() -> new IllegalArgumentException("Coleccion no encontrada"));

        boolean removido = coleccion.getProductos().removeIf(p -> p.getId().equals(productoId));
        if (!removido) {
            throw new IllegalArgumentException("El producto no pertenece a la coleccion");
        }
        if (coleccion.getProductos().isEmpty()) {
            throw new IllegalArgumentException("La coleccion debe tener al menos una pieza");
        }

        coleccion = coleccionRepository.save(coleccion);
        return toResponse(coleccion);
    }

    private Producto validarProductoParaColeccion(Integer productoId, Duenio duenio) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));

        if (!Producto.ESTADO_APROBADO.equalsIgnoreCase(producto.getEstadoInspeccion())) {
            throw new IllegalArgumentException("El producto " + productoId + " debe estar APROBADO");
        }
        if (producto.getDuenio() == null || !producto.getDuenio().getId().equals(duenio.getId())) {
            throw new IllegalArgumentException("El producto " + productoId + " no pertenece al duenio de la coleccion");
        }
        return producto;
    }

    private Catalogo obtenerOCrearCatalogo(Subasta subasta, String descripcion) {
        return catalogoRepository.findFirstBySubasta_Id(subasta.getId())
                .orElseGet(() -> {
                    Empleado responsable = empleadoRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new IllegalStateException("No hay empleados para crear el catalogo"));
                    return catalogoRepository.save(Catalogo.builder()
                            .descripcion(descripcion)
                            .subasta(subasta)
                            .responsable(responsable)
                            .build());
                });
    }

    private void agregarAlCatalogo(Catalogo catalogo, Producto producto, BigDecimal precioBase) {
        if (itemCatalogoRepository.existsByCatalogoIdAndProductoId(catalogo.getId(), producto.getId())) {
            return;
        }
        itemCatalogoRepository.save(ItemCatalogo.builder()
                .catalogo(catalogo)
                .producto(producto)
                .precioBase(precioBase.setScale(2, RoundingMode.HALF_UP))
                .comision(BigDecimal.ZERO)
                .subastado("no")
                .build());
    }

    private BigDecimal precioBaseDe(ProductoColeccionRequest item) {
        return precioBaseDe(item.productoId(), item.precioBase());
    }

    private BigDecimal precioBaseDe(Integer productoId, BigDecimal precioBase) {
        if (precioBase != null && precioBase.compareTo(BigDecimal.ZERO) > 0) {
            return precioBase;
        }
        return PRECIO_BASE_DEFAULT;
    }

    private ColeccionResponse toResponse(Coleccion coleccion) {
        List<Integer> ids = coleccion.getProductos().stream().map(Producto::getId).sorted().toList();
        String duenioNombre = coleccion.getDuenio().getPersona() != null
                ? coleccion.getDuenio().getPersona().getNombre()
                : null;
        return new ColeccionResponse(
                coleccion.getId(),
                coleccion.getNombre(),
                coleccion.getDuenio().getId(),
                duenioNombre,
                coleccion.getSubasta().getId(),
                ids,
                ids.size()
        );
    }

    private ColeccionResumenResponse toResumen(Coleccion coleccion) {
        String duenioNombre = coleccion.getDuenio().getPersona() != null
                ? coleccion.getDuenio().getPersona().getNombre()
                : null;
        return new ColeccionResumenResponse(
                coleccion.getId(),
                coleccion.getNombre(),
                coleccion.getSubasta().getId(),
                coleccion.getProductos().size(),
                duenioNombre
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
