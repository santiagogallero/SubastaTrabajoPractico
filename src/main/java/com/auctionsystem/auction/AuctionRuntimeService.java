package com.auctionsystem.auction;

import com.auctionsystem.auction.dto.PujaHistorialItem;
import com.auctionsystem.auction.dto.PujaResponse;
import com.auctionsystem.auction.dto.PujarRequest;
import com.auctionsystem.auction.dto.SubastaTimingResponse;
import com.auctionsystem.auth.MedioPagoRepository;
import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.auctionsystem.compliance.ComplianceService;
import com.auctionsystem.entities.Asistente;
import com.auctionsystem.entities.Cliente;
import com.auctionsystem.entities.ItemCatalogo;
import com.auctionsystem.entities.Pujo;
import com.auctionsystem.entities.Subasta;
import com.auctionsystem.repositories.AsistenteRepository;
import com.auctionsystem.repositories.ClienteRepository;
import com.auctionsystem.repositories.ItemCatalogoRepository;
import com.auctionsystem.repositories.PujoRepository;
import com.auctionsystem.repositories.SubastaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionRuntimeService {

    @Value("${app.auction.default-duration-minutes:120}")
    private int defaultDurationMinutes;

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final ClienteRepository clienteRepository;
    private final SubastaRepository subastaRepository;
    private final AsistenteRepository asistenteRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final PujoRepository pujoRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final ConexionSubastaActivaRepository conexionSubastaActivaRepository;
    private final SubastaConfigExtRepository subastaConfigExtRepository;
    private final ComplianceService complianceService;

    @Transactional
    public void configurarMoneda(Integer subastaId, String moneda) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new IllegalArgumentException("Subasta no encontrada"));

        String monedaNormalizada = normalizeCurrency(moneda);
        SubastaConfigExt config = subastaConfigExtRepository.findBySubastaId(subastaId)
                .orElseGet(SubastaConfigExt::new);
        config.setSubasta(subasta);
        config.setMoneda(monedaNormalizada);
        if (config.getDuracionMinutos() == null) {
            config.setDuracionMinutos(defaultDurationMinutes);
        }
        subastaConfigExtRepository.save(config);
    }

    @Transactional
    public void configurarDuracion(Integer subastaId, Integer duracionMinutos) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new IllegalArgumentException("Subasta no encontrada"));

        if (duracionMinutos == null || duracionMinutos < 1) {
            throw new IllegalArgumentException("La duracion debe ser mayor o igual a 1 minuto");
        }

        SubastaConfigExt config = subastaConfigExtRepository.findBySubastaId(subastaId)
                .orElseGet(SubastaConfigExt::new);
        config.setSubasta(subasta);
        if (config.getMoneda() == null) {
            config.setMoneda("ARS");
        }
        config.setDuracionMinutos(duracionMinutos);
        subastaConfigExtRepository.save(config);
    }

    @Transactional
    public void conectarASubasta(String email, Integer subastaId) {
        UsuarioAuth usuario = getUsuarioFromEmail(email);
        Cliente cliente = getClienteFromEmail(email);
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new IllegalArgumentException("Subasta no encontrada"));

        validateClientePuedeEntrar(usuario.getId(), cliente, subasta, false);

        ConexionSubastaActiva actual = conexionSubastaActivaRepository.findByClienteId(cliente.getId())
                .orElse(null);

        if (actual != null && !actual.getSubasta().getId().equals(subastaId)) {
            throw new IllegalArgumentException("El usuario no puede estar conectado en mas de una subasta a la vez");
        }

        if (actual == null) {
            ConexionSubastaActiva nueva = new ConexionSubastaActiva();
            nueva.setCliente(cliente);
            nueva.setSubasta(subasta);
            nueva.setConectadoAt(LocalDateTime.now());
            conexionSubastaActivaRepository.save(nueva);
        }
    }

    @Transactional
    public void desconectarDeSubasta(String email) {
        Cliente cliente = getClienteFromEmail(email);
        conexionSubastaActivaRepository.deleteByClienteId(cliente.getId());
    }

    @Transactional
    public PujaResponse pujar(String email, PujarRequest request) {
        UsuarioAuth usuario = getUsuarioFromEmail(email);
        Cliente cliente = getClienteFromEmail(email);
        Subasta subasta = subastaRepository.findById(request.subastaId())
                .orElseThrow(() -> new IllegalArgumentException("Subasta no encontrada"));

        validateClientePuedeEntrar(usuario.getId(), cliente, subasta, true);
        validateConexionActiva(cliente.getId(), subasta.getId());
        validateMonedaSubasta(request.subastaId(), request.moneda());

        Asistente asistente = asistenteRepository.findByClienteIdAndSubastaId(cliente.getId(), subasta.getId())
                .orElseThrow(() -> new IllegalArgumentException("El cliente no esta habilitado como asistente en la subasta"));

        ItemCatalogo item = itemCatalogoRepository.findByIdForUpdate(request.itemId())
                .orElseThrow(() -> new IllegalArgumentException("Item de catalogo no encontrado"));

        if (item.getCatalogo() == null || item.getCatalogo().getSubasta() == null
                || !item.getCatalogo().getSubasta().getId().equals(subasta.getId())) {
            throw new IllegalArgumentException("El item no pertenece a la subasta indicada");
        }

        if (subasta.getEstado() != null && !"ABIERTA".equalsIgnoreCase(subasta.getEstado())) {
            throw new IllegalArgumentException("Solo se puede pujar cuando la subasta esta ABIERTA");
        }

        BigDecimal base = item.getPrecioBase().setScale(2, RoundingMode.HALF_UP);
        BigDecimal ofertaAnterior = pujoRepository.findTopByItemIdOrderByImporteDesc(item.getId())
                .map(Pujo::getImporte)
                .orElse(base);

        BigDecimal ofertaActual = request.importe().setScale(2, RoundingMode.HALF_UP);
        if (ofertaActual.compareTo(ofertaAnterior) <= 0) {
            throw new IllegalArgumentException("La puja debe ser mayor a la mejor oferta actual");
        }

        BigDecimal minimo = ofertaAnterior;
        BigDecimal maximo = null;
        if (!isCategoriaSinLimites(subasta.getCategoria())) {
            minimo = ofertaAnterior.add(base.multiply(new BigDecimal("0.01"))).setScale(2, RoundingMode.HALF_UP);
            maximo = ofertaAnterior.add(base.multiply(new BigDecimal("0.20"))).setScale(2, RoundingMode.HALF_UP);
            if (ofertaActual.compareTo(minimo) < 0) {
                throw new IllegalArgumentException("La puja minima permitida es " + minimo);
            }
            if (ofertaActual.compareTo(maximo) > 0) {
                throw new IllegalArgumentException("La puja maxima permitida es " + maximo);
            }
        }

        Pujo pujo = Pujo.builder()
                .asistente(asistente)
                .item(item)
                .importe(ofertaActual)
                .ganador("no")
                .build();
        pujo = pujoRepository.save(pujo);

        return new PujaResponse(
                pujo.getId(),
                item.getId(),
                ofertaAnterior,
                ofertaActual,
                minimo,
                maximo,
                "Puja registrada correctamente"
        );
    }

    @Transactional(readOnly = true)
    public List<PujaHistorialItem> historial(Integer itemId) {
        return pujoRepository.findByItemIdOrderByIdAsc(itemId)
                .stream()
            .map(p -> new PujaHistorialItem(
                p.getId(),
                p.getAsistente().getId(),
                p.getAsistente().getNumeroPostor(),
                p.getAsistente().getCliente().getId(),
                p.getAsistente().getCliente().getPersona() != null
                    ? p.getAsistente().getCliente().getPersona().getNombre()
                    : null,
                p.getImporte(),
                p.getGanador()
            ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SubastaTimingResponse obtenerTimingSubasta(Integer subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new IllegalArgumentException("Subasta no encontrada"));

        int duracion = obtenerDuracionSubasta(subasta.getId());
        if (subasta.getFecha() == null || subasta.getHora() == null) {
            return new SubastaTimingResponse(subasta.getId(), duracion, null, null, "SIN_FECHA", null);
        }

        LocalDateTime inicio = LocalDateTime.of(subasta.getFecha(), subasta.getHora());
        LocalDateTime fin = inicio.plusMinutes(duracion);
        LocalDateTime ahora = LocalDateTime.now();

        String estado;
        Long minutosRestantes;
        if (ahora.isBefore(inicio)) {
            estado = "PROGRAMADA";
            minutosRestantes = Duration.between(ahora, inicio).toMinutes();
        } else if (ahora.isAfter(fin)) {
            estado = "FINALIZADA";
            minutosRestantes = 0L;
        } else {
            estado = "EN_CURSO";
            minutosRestantes = Duration.between(ahora, fin).toMinutes();
        }

        return new SubastaTimingResponse(subasta.getId(), duracion, inicio, fin, estado, minutosRestantes);
    }

    private Cliente getClienteFromEmail(String email) {
        UsuarioAuth usuario = getUsuarioFromEmail(email);

        if (usuario.getPersonaId() == null) {
            throw new IllegalArgumentException("Usuario sin persona asociada");
        }

        return clienteRepository.findByPersonaId(usuario.getPersonaId())
                .orElseThrow(() -> new IllegalArgumentException("No existe cliente asociado a la persona del usuario"));
    }

    private UsuarioAuth getUsuarioFromEmail(String email) {
        return usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no vinculado"));
    }

    private void validateClientePuedeEntrar(Long usuarioId, Cliente cliente, Subasta subasta, boolean requiereMedioPago) {
        if (complianceService.estaBloqueado(usuarioId)) {
            throw new IllegalArgumentException("Usuario bloqueado por mora de pago. Regularice para volver a participar");
        }

        validarVentanaTemporalSubasta(subasta);

        if (!"si".equalsIgnoreCase(cliente.getAdmitido())) {
            throw new IllegalArgumentException("Cliente no admitido por la empresa");
        }

        if (!categoriaHabilita(cliente.getCategoria(), subasta.getCategoria())) {
            throw new IllegalArgumentException("La categoria del cliente no habilita esta subasta");
        }

        if (requiereMedioPago) {
            boolean tieneMedioVerificado = medioPagoRepository
                    .existsByUsuarioIdAndVerificadoTrueAndActivoTrue(usuarioId);
            if (!tieneMedioVerificado) {
                throw new IllegalArgumentException("Debe tener al menos un medio de pago verificado y activo para pujar");
            }
        }
    }

    private void validateConexionActiva(Integer clienteId, Integer subastaId) {
        ConexionSubastaActiva conexion = conexionSubastaActivaRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Debe conectarse a la subasta antes de pujar"));
        if (!conexion.getSubasta().getId().equals(subastaId)) {
            throw new IllegalArgumentException("La conexion activa del usuario corresponde a otra subasta");
        }
    }

    private void validateMonedaSubasta(Integer subastaId, String monedaRequest) {
        String moneda = normalizeCurrency(monedaRequest);
        SubastaConfigExt config = subastaConfigExtRepository.findBySubastaId(subastaId)
                .orElseThrow(() -> new IllegalArgumentException("La subasta no tiene moneda configurada"));
        if (!config.getMoneda().equals(moneda)) {
            throw new IllegalArgumentException("La puja debe realizarse en moneda " + config.getMoneda());
        }
    }

    private String normalizeCurrency(String moneda) {
        String value = moneda.toUpperCase(Locale.ROOT).trim();
        if (!"ARS".equals(value) && !"USD".equals(value)) {
            throw new IllegalArgumentException("Moneda invalida. Valores permitidos: ARS, USD");
        }
        return value;
    }

    private boolean categoriaHabilita(String categoriaCliente, String categoriaSubasta) {
        int rankCliente = rankCategoria(categoriaCliente);
        int rankSubasta = rankCategoria(categoriaSubasta);
        return rankCliente >= rankSubasta;
    }

    private int rankCategoria(String categoria) {
        if (categoria == null) {
            return 0;
        }
        return switch (categoria.toUpperCase(Locale.ROOT)) {
            case "COMUN" -> 1;
            case "ESPECIAL" -> 2;
            case "PLATA" -> 3;
            case "ORO" -> 4;
            case "PLATINO" -> 5;
            default -> 0;
        };
    }

    private boolean isCategoriaSinLimites(String categoriaSubasta) {
        if (categoriaSubasta == null) {
            return false;
        }
        String normalized = categoriaSubasta.toUpperCase(Locale.ROOT);
        return "ORO".equals(normalized) || "PLATINO".equals(normalized);
    }

    private void validarVentanaTemporalSubasta(Subasta subasta) {
        if (subasta.getFecha() == null || subasta.getHora() == null) {
            return;
        }

        int duracion = obtenerDuracionSubasta(subasta.getId());

        LocalDateTime inicio = LocalDateTime.of(subasta.getFecha(), subasta.getHora());
        LocalDateTime fin = inicio.plusMinutes(duracion);
        LocalDateTime ahora = LocalDateTime.now();

        if (ahora.isBefore(inicio)) {
            throw new IllegalArgumentException("La subasta aun no comenzo. Inicia en " + inicio);
        }
        if (ahora.isAfter(fin)) {
            throw new IllegalArgumentException("La subasta finalizo segun su duracion configurada");
        }
    }

    private int obtenerDuracionSubasta(Integer subastaId) {
        return subastaConfigExtRepository.findBySubastaId(subastaId)
                .map(SubastaConfigExt::getDuracionMinutos)
                .filter(value -> value != null && value > 0)
                .orElse(defaultDurationMinutes);
    }
}
