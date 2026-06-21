package com.auctionsystem.auction;

import com.auctionsystem.compliance.ComplianceService;
import com.auctionsystem.auction.dto.AdjudicacionResponse;
import com.auctionsystem.auction.dto.ComisionResponse;
import com.auctionsystem.auction.dto.EntregaRequest;
import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.auctionsystem.entities.Cliente;
import com.auctionsystem.entities.ItemCatalogo;
import com.auctionsystem.entities.Producto;
import com.auctionsystem.entities.Pujo;
import com.auctionsystem.entities.RegistroDeSubasta;
import com.auctionsystem.entities.Subasta;
import com.auctionsystem.repositories.ClienteRepository;
import com.auctionsystem.repositories.ItemCatalogoRepository;
import com.auctionsystem.repositories.ProductoRepository;
import com.auctionsystem.repositories.PujoRepository;
import com.auctionsystem.repositories.RegistroDeSubastaRepository;
import com.auctionsystem.services.MailService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierre de venta / adjudicacion de un item subastado.
 *
 * <p>Determina el ganador (mejor puja), registra la venta con su comision,
 * marca la pieza como vendida, notifica por mensaje privado al comprador con el
 * importe a pagar (oferta + comision + envio) y permite consultar las
 * adjudicaciones del usuario. Si nadie pujo, la empresa adquiere la pieza por su
 * valor base.</p>
 */
@Service
@RequiredArgsConstructor
public class SaleService {

    @Value("${app.auction.shipping-flat:0}")
    private BigDecimal costoEnvioFlat;

    private final ItemCatalogoRepository itemCatalogoRepository;
    private final PujoRepository pujoRepository;
    private final RegistroDeSubastaRepository registroDeSubastaRepository;
    private final ProductoRepository productoRepository;
    private final SubastaConfigExtRepository subastaConfigExtRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final ComisionService comisionService;
    private final ComplianceService complianceService;
    private final MailService mailService;

    @Transactional
    public AdjudicacionResponse cerrarItem(Integer itemId) {
        ItemCatalogo item = itemCatalogoRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item de catalogo no encontrado"));

        if ("si".equalsIgnoreCase(item.getSubastado())) {
            throw new IllegalArgumentException("El item ya fue cerrado");
        }

        Subasta subasta = item.getCatalogo() != null ? item.getCatalogo().getSubasta() : null;
        if (subasta == null) {
            throw new IllegalArgumentException("El item no pertenece a ninguna subasta");
        }

        Producto producto = item.getProducto();
        BigDecimal base = item.getPrecioBase().setScale(2, RoundingMode.HALF_UP);
        String moneda = subastaConfigExtRepository.findBySubastaId(subasta.getId())
                .map(SubastaConfigExt::getMoneda)
                .orElse("ARS");

        item.setSubastado("si");
        itemCatalogoRepository.save(item);

        Optional<Pujo> mejorPuja = pujoRepository.findTopByItemIdOrderByImporteDesc(itemId);

        if (mejorPuja.isEmpty()) {
            return buildResponse(
                    null, itemId, producto.getId(), producto.getDescripcionCatalogo(),
                    null, null, base, BigDecimal.ZERO, BigDecimal.ZERO, base, moneda,
                    "COMPRADO_POR_EMPRESA",
                    "Sin ofertas. La empresa adquiere la pieza por su valor base de " + base + " " + moneda + ".",
                    null, null, null
            );
        }

        Pujo ganadora = mejorPuja.get();
        ganadora.setGanador("si");
        pujoRepository.save(ganadora);

        Cliente comprador = ganadora.getAsistente().getCliente();
        BigDecimal importe = ganadora.getImporte().setScale(2, RoundingMode.HALF_UP);
        ComisionResponse comisiones = comisionService.calcular(importe);
        BigDecimal comisionComprador = comisiones.comisionComprador();
        BigDecimal envio = costoEnvioFlat == null ? BigDecimal.ZERO : costoEnvioFlat.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = importe.add(comisionComprador).add(envio).setScale(2, RoundingMode.HALF_UP);

        RegistroDeSubasta registro = RegistroDeSubasta.builder()
                .subasta(subasta)
                .duenio(producto.getDuenio())
                .producto(producto)
                .cliente(comprador)
                .importe(importe)
                .comision(comisionComprador)
                .build();
        registro = registroDeSubastaRepository.save(registro);

        producto.setDisponible("no");
        productoRepository.save(producto);

        String nombre = comprador.getPersona() != null ? comprador.getPersona().getNombre() : null;
        String mensaje = "Felicitaciones, te adjudicaste la pieza. Debes abonar " + total + " " + moneda
                + " (oferta " + importe + " + comision " + comisionComprador + " + envio " + envio + ").";

        notificarGanador(comprador, mensaje);
        complianceService.inicializarPago(registro.getId());

        return buildResponse(
                registro.getId(), itemId, producto.getId(), producto.getDescripcionCatalogo(),
                comprador.getId(), nombre, importe, comisionComprador, envio, total, moneda,
                "VENDIDO", mensaje,
                registro.getModalidadEntrega(), registro.getDireccionEnvio(), registro.getSeguroVigenteTrasEntrega()
        );
    }

    @Transactional
    public AdjudicacionResponse seleccionarEntrega(String email, Integer registroId, EntregaRequest request) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (usuario.getPersonaId() == null) {
            throw new IllegalArgumentException("Usuario sin persona vinculada");
        }
        Cliente cliente = clienteRepository.findByPersonaId(usuario.getPersonaId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        RegistroDeSubasta registro = registroDeSubastaRepository.findById(registroId)
                .orElseThrow(() -> new IllegalArgumentException("Adjudicacion no encontrada"));
        if (!registro.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("No puede modificar una adjudicacion ajena");
        }

        String opcion = request != null && request.deliveryOption() != null
                ? request.deliveryOption().trim().toUpperCase(Locale.ROOT)
                : "SHIPPED";
        if (!"SHIPPED".equals(opcion) && !"PICKUP".equals(opcion)) {
            throw new IllegalArgumentException("deliveryOption debe ser SHIPPED o PICKUP");
        }

        registro.setModalidadEntrega(opcion);
        if ("PICKUP".equals(opcion)) {
            registro.setDireccionEnvio(null);
            registro.setSeguroVigenteTrasEntrega(false);
        } else {
            if (request.shippingAddress() == null || request.shippingAddress().isBlank()) {
                throw new IllegalArgumentException("shippingAddress es obligatoria para envio");
            }
            registro.setDireccionEnvio(request.shippingAddress().trim());
            registro.setSeguroVigenteTrasEntrega(true);
        }
        registro = registroDeSubastaRepository.save(registro);
        return toAdjudicacion(registro);
    }

    @Transactional(readOnly = true)
    public List<AdjudicacionResponse> misAdjudicaciones(String email) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (usuario.getPersonaId() == null) {
            return List.of();
        }

        Cliente cliente = clienteRepository.findByPersonaId(usuario.getPersonaId()).orElse(null);
        if (cliente == null) {
            return List.of();
        }

        return registroDeSubastaRepository.findByClienteId(cliente.getId()).stream()
                .map(this::toAdjudicacion)
                .toList();
    }

    private AdjudicacionResponse toAdjudicacion(RegistroDeSubasta registro) {
        BigDecimal importe = registro.getImporte();
        BigDecimal comision = registro.getComision() != null ? registro.getComision() : BigDecimal.ZERO;
        BigDecimal envio = costoEnvioFlat == null ? BigDecimal.ZERO : costoEnvioFlat;
        BigDecimal total = importe.add(comision).add(envio);
        String moneda = subastaConfigExtRepository.findBySubastaId(registro.getSubasta().getId())
                .map(SubastaConfigExt::getMoneda)
                .orElse("ARS");
        Producto producto = registro.getProducto();
        String nombre = registro.getCliente().getPersona() != null
                ? registro.getCliente().getPersona().getNombre()
                : null;

        return buildResponse(
                registro.getId(),
                null,
                producto != null ? producto.getId() : null,
                producto != null ? producto.getDescripcionCatalogo() : null,
                registro.getCliente().getId(),
                nombre,
                importe,
                comision,
                envio,
                total,
                moneda,
                "VENDIDO",
                "Adjudicacion registrada. Total a pagar: " + total + " " + moneda + ".",
                registro.getModalidadEntrega(),
                registro.getDireccionEnvio(),
                registro.getSeguroVigenteTrasEntrega()
        );
    }

    private AdjudicacionResponse buildResponse(
            Integer registroId,
            Integer itemId,
            Integer productoId,
            String productoDescripcion,
            Integer ganadorClienteId,
            String ganadorNombre,
            BigDecimal importe,
            BigDecimal comision,
            BigDecimal costoEnvio,
            BigDecimal totalPagar,
            String moneda,
            String estado,
            String mensaje,
            String modalidadEntrega,
            String direccionEnvio,
            Boolean seguroVigenteTrasEntrega
    ) {
        return new AdjudicacionResponse(
                registroId, itemId, productoId, productoDescripcion,
                ganadorClienteId, ganadorNombre, importe, comision, costoEnvio, totalPagar, moneda,
                estado, mensaje, modalidadEntrega, direccionEnvio, seguroVigenteTrasEntrega
        );
    }

    private void notificarGanador(Cliente comprador, String mensaje) {
        if (comprador.getPersona() == null) {
            return;
        }
        usuarioAuthRepository.findByPersonaId(comprador.getPersona().getId())
                .ifPresent(u -> mailService.sendPlainText(u.getEmail(), "Adjudicacion de subasta", mensaje));
    }
}
