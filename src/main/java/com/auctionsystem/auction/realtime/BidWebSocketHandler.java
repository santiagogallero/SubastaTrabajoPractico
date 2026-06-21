package com.auctionsystem.auction.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Mantiene las sesiones WebSocket abiertas agrupadas por subasta y difunde los
 * eventos de puja a todos los participantes conectados a esa misma subasta.
 *
 * <p>El identificador de la subasta se resuelve en el handshake
 * ({@link AuctionHandshakeInterceptor}) y queda guardado en los atributos de la
 * sesion.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BidWebSocketHandler extends TextWebSocketHandler {

    public static final String ATTR_SUBASTA_ID = "subastaId";
    public static final String ATTR_USERNAME = "username";

    private final ObjectMapper objectMapper;

    private final Map<Integer, Set<WebSocketSession>> sessionsBySubasta = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer subastaId = subastaIdDe(session);
        if (subastaId == null) {
            cerrarSilencioso(session);
            return;
        }
        sessionsBySubasta.computeIfAbsent(subastaId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("WS conectado a subasta {} (sesiones={})", subastaId, sessionsBySubasta.get(subastaId).size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer subastaId = subastaIdDe(session);
        if (subastaId == null) {
            return;
        }
        Set<WebSocketSession> sesiones = sessionsBySubasta.get(subastaId);
        if (sesiones != null) {
            sesiones.remove(session);
            if (sesiones.isEmpty()) {
                sessionsBySubasta.remove(subastaId);
            }
        }
    }

    /**
     * Difunde un evento a todas las sesiones conectadas a la subasta indicada.
     */
    public void difundir(Integer subastaId, BidEvent evento) {
        Set<WebSocketSession> sesiones = sessionsBySubasta.get(subastaId);
        if (sesiones == null || sesiones.isEmpty()) {
            return;
        }
        TextMessage mensaje;
        try {
            mensaje = new TextMessage(objectMapper.writeValueAsString(evento));
        } catch (IOException e) {
            log.warn("No se pudo serializar el evento de puja", e);
            return;
        }
        for (WebSocketSession session : sesiones) {
            enviar(session, mensaje);
        }
    }

    private void enviar(WebSocketSession session, TextMessage mensaje) {
        if (!session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(mensaje);
            }
        } catch (IOException e) {
            log.debug("No se pudo enviar el evento a la sesion {}", session.getId(), e);
        }
    }

    private Integer subastaIdDe(WebSocketSession session) {
        Object value = session.getAttributes().get(ATTR_SUBASTA_ID);
        return value instanceof Integer ? (Integer) value : null;
    }

    private void cerrarSilencioso(WebSocketSession session) {
        try {
            session.close(CloseStatus.BAD_DATA);
        } catch (IOException ignored) {
            // nada que hacer
        }
    }
}
