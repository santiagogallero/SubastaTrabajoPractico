package com.auctionsystem.catalog;

import com.auctionsystem.auction.SubastaConfigExt;
import com.auctionsystem.auction.SubastaConfigExtRepository;
import com.auctionsystem.catalog.dto.CatalogoItemResponse;
import com.auctionsystem.catalog.dto.CatalogoResponse;
import com.auctionsystem.entities.Catalogo;
import com.auctionsystem.entities.ItemCatalogo;
import com.auctionsystem.entities.Producto;
import com.auctionsystem.entities.Subasta;
import com.auctionsystem.repositories.CatalogoRepository;
import com.auctionsystem.repositories.ItemCatalogoRepository;
import com.auctionsystem.repositories.SubastaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final SubastaRepository subastaRepository;
    private final CatalogoRepository catalogoRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final SubastaConfigExtRepository subastaConfigExtRepository;

    @Transactional(readOnly = true)
    public CatalogoResponse obtenerCatalogo(Integer subastaId, boolean mostrarPrecioBase) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new IllegalArgumentException("Subasta no encontrada"));

        Catalogo catalogo = catalogoRepository.findFirstBySubasta_Id(subastaId)
                .orElseThrow(() -> new IllegalArgumentException("La subasta no tiene catalogo"));

        String moneda = subastaConfigExtRepository.findBySubastaId(subastaId)
                .map(SubastaConfigExt::getMoneda)
                .orElse("ARS");

        List<ItemCatalogo> items = itemCatalogoRepository.findByCatalogoIdOrderByIdAsc(catalogo.getId());
        List<CatalogoItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponse(item, moneda, mostrarPrecioBase))
                .toList();

        return new CatalogoResponse(
                subasta.getId(),
                catalogo.getId(),
                catalogo.getDescripcion(),
                moneda,
                itemResponses
        );
    }

    private CatalogoItemResponse toItemResponse(ItemCatalogo item, String moneda, boolean mostrarPrecioBase) {
        Producto producto = item.getProducto();
        String titulo = producto != null && producto.getTitulo() != null
                ? producto.getTitulo()
                : (producto != null ? producto.getDescripcionCatalogo() : null);
        String descripcion = producto != null ? producto.getDescripcionCompleta() : null;
        String categoria = producto != null ? producto.getCategoria() : null;
        boolean tieneSeguro = producto != null && producto.getSeguro() != null;
        String duenioNombre = producto != null && producto.getDuenio() != null
                && producto.getDuenio().getPersona() != null
                ? producto.getDuenio().getPersona().getNombre()
                : null;

        return new CatalogoItemResponse(
                item.getId(),
                producto != null ? producto.getId() : null,
                titulo,
                descripcion,
                categoria,
                mostrarPrecioBase ? item.getPrecioBase() : null,
                moneda,
                tieneSeguro,
                duenioNombre
        );
    }
}
