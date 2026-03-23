package com.auctionsystem.compliance;

import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.auctionsystem.compliance.dto.PagoEstadoDto;
import com.auctionsystem.entities.Cliente;
import com.auctionsystem.entities.RegistroDeSubasta;
import com.auctionsystem.repositories.RegistroDeSubastaRepository;
import com.auctionsystem.services.MailService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
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
    private final MailService mailService;

    @Value("${app.auction.penalty.percent:10}")
    private BigDecimal porcentajeMulta;

    @Value("${app.auction.penalty.grace-hours:72}")
    private long horasGracia;

    @Value("${app.auction.penalty.reminder-hours:24}")
    private long horasRecordatorio;

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

        PagoSubastaExt pago = pagoRepository.findByRegistroSubastaId(registroSubastaId)
                .orElseGet(PagoSubastaExt::new);

        pago.setRegistroSubasta(registro);
        pago.setUsuario(usuario);
        pago.setEstadoPago("PENDIENTE");
        pago.setMontoOfertado(registro.getImporte().setScale(2, RoundingMode.HALF_UP));
        pago.setMontoMulta(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        pago.setMultaAplicada(false);
        pago.setNotificadoMora(false);
        pago.setNotificadoVencimiento(false);
        pago.setFechaVencimiento(LocalDateTime.now().plusHours(horasGracia));
        pago.setFechaLimiteRegularizacion(null);
        pago.setFechaPago(null);
        pago = pagoRepository.save(pago);

        return toDto(pago);
    }

    @Transactional
    public PagoEstadoDto registrarPago(Integer registroSubastaId) {
        PagoSubastaExt pago = pagoRepository.findByRegistroSubastaId(registroSubastaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe estado de pago para este registro"));

        pago.setEstadoPago("PAGADO");
        pago.setFechaPago(LocalDateTime.now());
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
        return new PagoEstadoDto(
                pago.getRegistroSubasta().getId(),
                pago.getEstadoPago(),
                pago.getMontoOfertado(),
                pago.getMontoMulta(),
                pago.getFechaVencimiento(),
                pago.getFechaLimiteRegularizacion(),
                bloqueado
        );
    }
}
