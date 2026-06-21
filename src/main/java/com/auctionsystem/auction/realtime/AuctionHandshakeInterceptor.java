package com.auctionsystem.auction.realtime;

import com.auctionsystem.auth.CustomUserDetailsService;
import com.auctionsystem.auth.JwtService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Valida la conexion WebSocket durante el handshake. Como React Native no
 * permite enviar headers en el handshake, el token JWT y el id de subasta
 * viajan como query params (?token=...&subastaId=...). Si el token es valido se
 * guardan el usuario y la subasta en los atributos de la sesion.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuctionHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Map<String, String> params = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().toSingleValueMap();

        String token = params.get("token");
        String subastaIdRaw = params.get("subastaId");
        if (token == null || subastaIdRaw == null) {
            log.debug("Handshake WS rechazado: faltan token o subastaId");
            return false;
        }

        Integer subastaId;
        try {
            subastaId = Integer.valueOf(subastaIdRaw);
        } catch (NumberFormatException e) {
            return false;
        }

        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtService.isTokenValid(token, userDetails)) {
                return false;
            }
            attributes.put(BidWebSocketHandler.ATTR_USERNAME, username);
            attributes.put(BidWebSocketHandler.ATTR_SUBASTA_ID, subastaId);
            return true;
        } catch (Exception e) {
            log.debug("Handshake WS rechazado: token invalido", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // sin acciones posteriores
    }
}
