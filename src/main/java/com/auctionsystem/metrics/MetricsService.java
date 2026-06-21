package com.auctionsystem.metrics;

import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.auctionsystem.entities.Cliente;
import com.auctionsystem.entities.Pujo;
import com.auctionsystem.entities.RegistroDeSubasta;
import com.auctionsystem.repositories.AsistenteRepository;
import com.auctionsystem.repositories.ClienteRepository;
import com.auctionsystem.repositories.PujoRepository;
import com.auctionsystem.repositories.RegistroDeSubastaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final ClienteRepository clienteRepository;
    private final AsistenteRepository asistenteRepository;
    private final PujoRepository pujoRepository;
    private final RegistroDeSubastaRepository registroDeSubastaRepository;

    @Transactional(readOnly = true)
    public MetricasUsuarioResponse metricasDe(String email) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cliente cliente = usuario.getPersonaId() == null
                ? null
                : clienteRepository.findByPersonaId(usuario.getPersonaId()).orElse(null);

        if (cliente == null) {
            return new MetricasUsuarioResponse(null, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Integer clienteId = cliente.getId();

        long subastasParticipadas = asistenteRepository.findByClienteId(clienteId).size();

        List<Pujo> pujas = pujoRepository.findByAsistenteClienteId(clienteId);
        long pujasRealizadas = pujas.size();
        BigDecimal totalOfertado = pujas.stream()
                .map(Pujo::getImporte)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RegistroDeSubasta> ganadas = registroDeSubastaRepository.findByClienteId(clienteId);
        long subastasGanadas = ganadas.size();
        BigDecimal totalPagado = ganadas.stream()
                .map(r -> r.getImporte().add(r.getComision() != null ? r.getComision() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MetricasUsuarioResponse(
                cliente.getCategoria(),
                subastasParticipadas,
                pujasRealizadas,
                subastasGanadas,
                totalOfertado,
                totalPagado
        );
    }
}
