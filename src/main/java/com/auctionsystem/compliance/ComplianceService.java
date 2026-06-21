package com.auctionsystem.compliance;

import com.auctionsystem.auth.MedioPago;
import com.auctionsystem.auth.MedioPagoRepository;
import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.auctionsystem.auction.SubastaConfigExt;
import com.auctionsystem.auction.SubastaConfigExtRepository;
import com.auctionsystem.compliance.dto.EjecutarPagoRequest;
import com.auctionsystem.compliance.dto.EjecutarPagoResponse;
import com.auctionsystem.compliance.dto.PagoEstadoDto;
import com.auctionsystem.entities.Cliente;
import com.auctionsystem.entities.Producto;
import com.auctionsystem.entities.RegistroDeSubasta;
import com.auctionsystem.repositories.ClienteRepository;
import com.auctionsystem.repositories.RegistroDeSubastaRepository;
import com.auctionsystem.services.MailService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final PagoSubastaExtRepository pagoRepository;
    private final BloqueoParticipacionRepository bloqueoRepository;
    private final RegistroDeSubastaRepository registroRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final ClienteRepository clienteRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final SubastaConfigExtRepository subastaConfigExtRepository;
    private final MailService mailService;

    @Value("${app.auction.penalty.percent:10}")
    private BigDecimal porcentajeMulta;

    @Value("${app.auction.penalty.grace-hours:72}")
    private long horasGracia;

    @Value("${app.auction.penalty.reminder-hours:24}")
    private long horasRecordatorio;

    @Value("${app.auction.shipping-flat:0}")
    private BigDecimal costoEnvioFlat;

    @Transactional
    public PagoEstadoDto inicializarPago(Integer registroSubastaId) {
        RegistroDeSubasta registro = registroRepository.findById(registroSubastaId)
                .orElseThrow(() -> new IllegalArgumentException("Registro de subasta no encontrado"));

        Cliente cliente = registro.getCliente();
        if (cliente == null || cliente.getPersona() == null) {
            throw new IllegalArgumentException("El registro no tiene cliente/persona asociado");
        }

        UsuarioAuth usuario = usuarioAuthRepository.findByPersonaId(cliente.getPersona().getId())
                .orElseThrow(() -> new IllegalArgumentException("No existe usuario auth para el cliente comprador"));

        Totales totales = calcularTotales(registro);

        PagoSubastaExt pago = pagoRepository.findByRegistroSubastaId(registroSubastaId)
                .orElseGet(PagoSubastaExt::new);

        pago.setRegistroSubasta(registro);
        pago.setUsuario(usuario);
        pago.setEstadoPago("PENDIENTE");
        pago.setMontoOfertado(totales.importe());
        pago.setMontoTotal(totales.total());
        pago.setMoneda(totales.moneda());
        pago.setProductoDescripcion(descripcionProducto(registro.getProducto()));
        pago.setMontoMulta(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        pago.setMultaAplicada(false);
        pago.setNotificadoMora(false);
        pago.setNotificadoVencimiento(false);
        pago.setFechaVencimiento(LocalDateTime.now().plusHours(horasGracia));
        pago.setFechaLimiteRegularizacion(null);
        pago.setFechaPago(null);
        pago.setMedioPagoId(null);
        pago.setTransaccionId(null);
        pago = pagoRepository.save(pago);

        return toDto(pago);
    }

    @Transactional(readOnly = true)
    public PagoEstadoDto obtenerCheckout(String email, Integer registroSubastaId) {
        validarPropiedadRegistro(email, registroSubastaId);
        PagoSubastaExt pago = pagoRepository.findByRegistroSubastaId(registroSubastaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay pago pendiente para esta adjudicacion. Contacte al administrador."));
        return toDto(pago);
    }

    @Transactional
    public PagoEstadoDto asegurarCheckout(String email, Integer registroSubastaId) {
        validarPropiedadRegistro(email, registroSubastaId);
        return pagoRepository.findByRegistroSubastaId(registroSubastaId)
                .map(this::toDto)
                .orElseGet(() -> inicializarPago(registroSubastaId));
    }

    @Transactional
    public EjecutarPagoResponse ejecutarPago(String email, EjecutarPagoRequest request) {
        validarPropiedadRegistro(email, request.registroSubastaId());

        PagoSubastaExt pago = pagoRepository.findByRegistroSubastaId(request.registroSubastaId())
                .orElseGet(() -> {
                    inicializarPago(request.registroSubastaId());
                    return pagoRepository.findByRegistroSubastaId(request.registroSubastaId())
                            .orElseThrow(() -> new IllegalArgumentException("No se pudo inicializar el pago"));
                });

        if ("PAGADO".equalsIgnoreCase(pago.getEstadoPago())) {
            throw new IllegalArgumentException("Esta adjudicacion ya fue pagada");
        }
        if (!"PENDIENTE".equalsIgnoreCase(pago.getEstadoPago())
                && !"VENCIDO".equalsIgnoreCase(pago.getEstadoPago())) {
            throw new IllegalArgumentException("El pago no puede procesarse en estado " + pago.getEstadoPago());
        }

        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        MedioPago medioPago = medioPagoRepository.findById(request.medioPagoId())
                .orElseThrow(() -> new IllegalArgumentException("Medio de pago no encontrado"));

        if (!medioPago.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("El medio de pago no pertenece al usuario");
        }
        if (!Boolean.TRUE.equals(medioPago.getVerificado())) {
            throw new IllegalArgumentException("El medio de pago no esta verificado");
        }
        if (!Boolean.TRUE.equals(medioPago.getActivo())) {
            throw new IllegalArgumentException("El medio de pago no esta activo");
        }

        BigDecimal montoAPagar = montoAPagar(pago);
        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        LocalDateTime ahora = LocalDateTime.now();

        pago.setEstadoPago("PAGADO");
        pago.setFechaPago(ahora);
        pago.setMedioPagoId(medioPago.getId());
        pago.setTransaccionId(txnId);
        pagoRepository.save(pago);

        desbloquearUsuario(pago.getUsuario(), "Pago regularizado");

        mailService.sendPlainText(
                pago.getUsuario().getEmail(),
                "Pago registrado",
                "Se confirmo el pago de " + montoAPagar + " " + pago.getMoneda()
                        + " por " + pago.getProductoDescripcion()
                        + ". Transaccion: " + txnId + "."
        );

        return new EjecutarPagoResponse(
                true,
                txnId,
                montoAPagar,
                pago.getMoneda(),
                pago.getProductoDescripcion(),
                medioPago.getAliasDescripcion(),
                ahora
        );
    }

    @Transactional
    public PagoEstadoDto registrarPago(Integer registroSubastaId) {
        PagoSubastaExt pago = pagoRepository.findByRegistroSubastaId(registroSubastaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe estado de pago para este registro"));

        pago.setEstadoPago("PAGADO");
        pago.setFechaPago(LocalDateTime.now());
        if (pago.getTransaccionId() == null) {
            pago.setTransaccionId("TXN-ADMIN-" + registroSubastaId);
        }
        pagoRepository.save(pago);

        desbloquearUsuario(pago.getUsuario(), "Pago regularizado");

        mailService.sendPlainText(
                pago.getUsuario().getEmail(),
                "Pago registrado",
                "Se confirmo el pago de tu subasta. Ya podes participar normalmente."
        );

        return toDto(pago);
    }

    @Transactional(readOnly = true)
    public List<PagoEstadoDto> estadoUsuario(String email) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return pagoRepository.findByUsuarioIdOrderByFechaVencimientoDesc(usuario.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public int procesarMorasAhora() {
        return ejecutarRecordatoriosYMora();
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void procesoProgramado() {
        ejecutarRecordatoriosYMora();
    }

    private int ejecutarRecordatoriosYMora() {
        LocalDateTime ahora = LocalDateTime.now();

        List<PagoSubastaExt> porVencer = pagoRepository.findByEstadoPagoAndFechaVencimientoBetween(
                "PENDIENTE",
                ahora,
                ahora.plusHours(horasRecordatorio)
        );

        for (PagoSubastaExt pago : porVencer) {
            if (!Boolean.TRUE.equals(pago.getNotificadoVencimiento())) {
                mailService.sendPlainText(
                        pago.getUsuario().getEmail(),
                        "Recordatorio de pago",
                        "Tenes una compra pendiente. Si no pagas antes de " + pago.getFechaVencimiento()
                                + " se aplicara una multa del " + porcentajeMulta + "% y bloqueo temporal."
                );
                pago.setNotificadoVencimiento(true);
                pagoRepository.save(pago);
            }
        }

        List<PagoSubastaExt> vencidos = pagoRepository.findByEstadoPagoAndFechaVencimientoBefore("PENDIENTE", ahora);
        int procesados = 0;
        for (PagoSubastaExt pago : vencidos) {
            BigDecimal multa = pago.getMontoOfertado()
                    .multiply(porcentajeMulta)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            pago.setEstadoPago("VENCIDO");
            pago.setMultaAplicada(true);
            pago.setMontoMulta(multa);
            pago.setFechaLimiteRegularizacion(ahora.plusHours(horasGracia));
            pagoRepository.save(pago);

            bloquearUsuario(pago.getUsuario(), "Mora en pago de subasta. Debe pagar oferta + multa");

            if (!Boolean.TRUE.equals(pago.getNotificadoMora())) {
                mailService.sendPlainText(
                        pago.getUsuario().getEmail(),
                        "Multa aplicada por falta de pago",
                        "Tu pago vencio. Se aplico multa de " + multa +
                                ". Debes regularizar antes de " + pago.getFechaLimiteRegularizacion() +
                                " para evitar acciones judiciales."
                );
                pago.setNotificadoMora(true);
                pagoRepository.save(pago);
            }

            procesados++;
        }

        return procesados;
    }

    private void validarPropiedadRegistro(String email, Integer registroSubastaId) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (usuario.getPersonaId() == null) {
            throw new IllegalArgumentException("Usuario sin persona vinculada");
        }
        Cliente cliente = clienteRepository.findByPersonaId(usuario.getPersonaId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        RegistroDeSubasta registro = registroRepository.findById(registroSubastaId)
                .orElseThrow(() -> new IllegalArgumentException("Adjudicacion no encontrada"));
        if (!registro.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("No puede pagar una adjudicacion ajena");
        }
    }

    private Totales calcularTotales(RegistroDeSubasta registro) {
        BigDecimal importe = registro.getImporte().setScale(2, RoundingMode.HALF_UP);
        BigDecimal comision = registro.getComision() != null
                ? registro.getComision().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal envio = costoEnvioFlat == null ? BigDecimal.ZERO : costoEnvioFlat.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = importe.add(comision).add(envio).setScale(2, RoundingMode.HALF_UP);
        String moneda = subastaConfigExtRepository.findBySubastaId(registro.getSubasta().getId())
                .map(SubastaConfigExt::getMoneda)
                .orElse("ARS");
        return new Totales(importe, total, moneda);
    }

    private BigDecimal montoAPagar(PagoSubastaExt pago) {
        BigDecimal base = pago.getMontoTotal() != null ? pago.getMontoTotal() : pago.getMontoOfertado();
        if ("VENCIDO".equalsIgnoreCase(pago.getEstadoPago())) {
            BigDecimal multa = pago.getMontoMulta() != null ? pago.getMontoMulta() : BigDecimal.ZERO;
            return base.add(multa).setScale(2, RoundingMode.HALF_UP);
        }
        return base.setScale(2, RoundingMode.HALF_UP);
    }

    private String descripcionProducto(Producto producto) {
        if (producto == null) {
            return null;
        }
        if (producto.getTitulo() != null && !producto.getTitulo().isBlank()) {
            return producto.getTitulo().trim();
        }
        return producto.getDescripcionCatalogo();
    }

    private void bloquearUsuario(UsuarioAuth usuario, String motivo) {
        BloqueoParticipacion bloqueo = bloqueoRepository.findByUsuarioId(usuario.getId())
                .orElseGet(BloqueoParticipacion::new);
        bloqueo.setUsuario(usuario);
        bloqueo.setActivo(true);
        bloqueo.setMotivo(motivo);
        bloqueo.setUpdatedAt(LocalDateTime.now());
        bloqueoRepository.save(bloqueo);
    }

    private void desbloquearUsuario(UsuarioAuth usuario, String motivo) {
        BloqueoParticipacion bloqueo = bloqueoRepository.findByUsuarioId(usuario.getId())
                .orElse(null);
        if (bloqueo != null) {
            bloqueo.setActivo(false);
            bloqueo.setMotivo(motivo);
            bloqueo.setUpdatedAt(LocalDateTime.now());
            bloqueoRepository.save(bloqueo);
        }
    }

    public boolean estaBloqueado(Long usuarioId) {
        return bloqueoRepository.existsByUsuarioIdAndActivoTrue(usuarioId);
    }

    private PagoEstadoDto toDto(PagoSubastaExt pago) {
        boolean bloqueado = bloqueoRepository.existsByUsuarioIdAndActivoTrue(pago.getUsuario().getId());
        BigDecimal multaPotencial = pago.getMontoOfertado()
                .multiply(porcentajeMulta)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal montoTotal = pago.getMontoTotal() != null ? pago.getMontoTotal() : pago.getMontoOfertado();
        return new PagoEstadoDto(
                pago.getRegistroSubasta().getId(),
                pago.getEstadoPago(),
                pago.getMontoOfertado(),
                montoTotal,
                pago.getMontoMulta(),
                multaPotencial,
                pago.getMoneda(),
                pago.getProductoDescripcion(),
                pago.getTransaccionId(),
                pago.getFechaVencimiento(),
                pago.getFechaLimiteRegularizacion(),
                bloqueado
        );
    }

    private record Totales(BigDecimal importe, BigDecimal total, String moneda) {
    }
}
