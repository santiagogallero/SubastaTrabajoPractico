package com.auctionsystem.auction;

import com.auctionsystem.auction.dto.CierreSubastaResponse;
import com.auctionsystem.auction.dto.PujaHistorialItem;
import com.auctionsystem.auction.dto.PujaResponse;
import com.auctionsystem.auction.dto.PujarRequest;
import com.auctionsystem.auction.dto.StreamingResponse;
import com.auctionsystem.auction.dto.SubastaTimingResponse;
import com.auctionsystem.auction.realtime.BidEvent;
import com.auctionsystem.auction.realtime.BidWebSocketHandler;
import com.auctionsystem.auth.MedioPagoRepository;
import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.auctionsystem.compliance.ComplianceService;
import com.auctionsystem.entities.Asistente;
import com.auctionsystem.entities.Cliente;
import com.auctionsystem.entities.Duenio;
import com.auctionsystem.entities.ItemCatalogo;
import com.auctionsystem.entities.Pujo;
import com.auctionsystem.entities.RegistroDeSubasta;
import com.auctionsystem.entities.Subasta;
import com.auctionsystem.repositories.AsistenteRepository;
import com.auctionsystem.repositories.ClienteRepository;
import com.auctionsystem.repositories.DuenioRepository;
import com.auctionsystem.repositories.ItemCatalogoRepository;
import com.auctionsystem.repositories.PujoRepository;
import com.auctionsystem.repositories.RegistroDeSubastaRepository;
import com.auctionsystem.repositories.SubastaRepository;
import com.auctionsystem.services.MailService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class AuctionRuntimeService {

    @Value("${app.auction.default-duration-minutes:120}")
    private int defaultDurationMinutes;

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final ClienteRepository clienteRepository;
    private final DuenioRepository duenioRepository;
    private final SubastaRepository subastaRepository;
    private final AsistenteRepository asistenteRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final PujoRepository pujoRepository;
    private final RegistroDeSubastaRepository registroRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final ConexionSubastaActivaRepository conexionSubastaActivaRepository;
    private final SubastaConfigExtRepository subastaConfigExtRepository;
    private final ComplianceService complianceService;
    private final BidWebSocketHandler bidWebSocketHandler;
    private final MailService mailService;

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

        asegurarAsistente(cliente, subasta);
    }

    private void asegurarAsistente(Cliente cliente, Subasta subasta) {
        asistenteRepository.findByClienteIdAndSubastaId(cliente.getId(), subasta.getId())
                .orElseGet(() -> {
                    int numeroPostor = (int) (asistenteRepository.countBySubastaId(subasta.getId()) + 1);
                    Asistente asistente = Asistente.builder()
                            .cliente(cliente)
                            .subasta(subasta)
                            .numeroPostor(numeroPostor)
                            .build();
                    return asistenteRepository.save(asistente);
                });
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

        if (!isSubastaAbierta(subasta.getEstado())) {
            throw new IllegalArgumentException("Solo se puede pujar cuando la subasta esta abierta");
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

        // Limites que debera respetar la *proxima* puja, calculados sobre la
        // oferta recien aceptada (es lo que necesitan ver los demas postores).
        BigDecimal incremento = base.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal siguienteMinimo = ofertaActual.add(incremento).setScale(2, RoundingMode.HALF_UP);
        BigDecimal siguienteMaximo = isCategoriaSinLimites(subasta.getCategoria())
                ? null
                : ofertaActual.add(base.multiply(new BigDecimal("0.20"))).setScale(2, RoundingMode.HALF_UP);

        String nombrePostor = asistente.getCliente().getPersona() != null
                ? asistente.getCliente().getPersona().getNombre()
                : null;
        BidEvent evento = new BidEvent(
                BidEvent.TIPO_NUEVA_PUJA,
                subasta.getId(),
                item.getId(),
                pujo.getId(),
                asistente.getNumeroPostor(),
                nombrePostor,
                ofertaActual,
                siguienteMinimo,
                siguienteMaximo,
                LocalDateTime.now().toString()
        );
        difundirPujaTrasCommit(subasta.getId(), evento);

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

    /**
     * Difunde el evento solo si la transaccion confirma, para no notificar
     * pujas que terminen revirtiendose.
     */
    private void difundirPujaTrasCommit(Integer subastaId, BidEvent evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    bidWebSocketHandler.difundir(subastaId, evento);
                }
            });
        } else {
            bidWebSocketHandler.difundir(subastaId, evento);
        }
    }

    @Transactional
    public CierreSubastaResponse cerrarSubasta(Integer subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new IllegalArgumentException("Subasta no encontrada"));

        if ("CERRADA".equalsIgnoreCase(subasta.getEstado())) {
            throw new IllegalArgumentException("La subasta ya fue cerrada");
        }

        List<ItemCatalogo> items = itemCatalogoRepository.findBySubastaId(subastaId);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("La subasta no tiene items en catalogo");
        }

        List<CierreSubastaResponse.ItemCierreDto> detalle = new ArrayList<>();
        int vendidos = 0;
        int sinOfertas = 0;

        for (ItemCatalogo item : items) {
            Pujo pujoGanador = pujoRepository.findTopByItemIdOrderByImporteDesc(item.getId()).orElse(null);

            if (pujoGanador != null) {
                // Marcar puja ganadora
                pujoGanador.setGanador("si");
                pujoRepository.save(pujoGanador);

                // Marcar item como subastado
                item.setSubastado("si");
                itemCatalogoRepository.save(item);

                Cliente comprador = pujoGanador.getAsistente().getCliente();
                Duenio duenio = item.getProducto().getDuenio();

                // Crear registro de venta
                RegistroDeSubasta registro = RegistroDeSubasta.builder()
                        .subasta(subasta)
                        .duenio(duenio)
                        .producto(item.getProducto())
                        .cliente(comprador)
                        .importe(pujoGanador.getImporte())
                        .comision(item.getComision())
                        .build();
                registro = registroRepository.save(registro);

                // Disparar el reloj de pago (72 horas para presentar fondos)
                complianceService.inicializarPago(registro.getId());

                // Notificar al ganador
                String emailGanador = usuarioAuthRepository.findByPersonaId(comprador.getPersona().getId())
                        .map(u -> u.getEmail())
                        .orElse(null);

                String nombreGanador = comprador.getPersona() != null ? comprador.getPersona().getNombre() : "Comprador";

                if (emailGanador != null) {
                    BigDecimal total = pujoGanador.getImporte().add(item.getComision());
                    mailService.sendPlainText(
                            emailGanador,
                            "¡Ganaste la subasta!",
                            "Felicitaciones " + nombreGanador + "!\n\n"
                            + "Ganaste el lote: " + item.getProducto().getDescripcionCompleta() + "\n"
                            + "Importe ofertado: " + pujoGanador.getImporte() + "\n"
                            + "Comision: " + item.getComision() + "\n"
                            + "Total a pagar: " + total + "\n\n"
                            + "Tenes 72 horas para presentar los fondos. "
                            + "Si no pagas en ese plazo se aplicara una multa del 10% del importe ofertado "
                            + "y quedaras bloqueado para participar en futuras subastas."
                    );
                }

                detalle.add(new CierreSubastaResponse.ItemCierreDto(
                        item.getId(),
                        item.getProducto().getDescripcionCompleta(),
                        item.getPrecioBase(),
                        pujoGanador.getImporte(),
                        emailGanador,
                        nombreGanador,
                        "VENDIDO"
                ));
                vendidos++;

            } else {
                // Sin ofertas: empresa compra al precio base
                item.setSubastado("si");
                itemCatalogoRepository.save(item);

                detalle.add(new CierreSubastaResponse.ItemCierreDto(
                        item.getId(),
                        item.getProducto().getDescripcionCompleta(),
                        item.getPrecioBase(),
                        item.getPrecioBase(),
                        null,
                        "Empresa",
                        "SIN_OFERTAS"
                ));
                sinOfertas++;
            }
        }

        // Cerrar la subasta
        subasta.setEstado("CERRADA");
        subastaRepository.save(subasta);

        // Desconectar a todos los asistentes
        conexionSubastaActivaRepository.deleteBySubastaId(subastaId);

        return new CierreSubastaResponse(subastaId, vendidos, sinOfertas, detalle);
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

    @Transactional(readOnly = true)
    public StreamingResponse obtenerStreaming(Integer subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new IllegalArgumentException("Subasta no encontrada"));
        return new StreamingResponse(subasta.getId(), subasta.getStreamingUrl());
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

    private boolean isSubastaAbierta(String estado) {
        if (estado == null) {
            return true;
        }
        String normalized = estado.toUpperCase(Locale.ROOT);
        return "ABIERTA".equals(normalized) || "ACTIVA".equals(normalized) || "EN_CURSO".equals(normalized);
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
