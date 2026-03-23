package com.auctionsystem.chat;

import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.auctionsystem.chat.dto.ConversacionDto;
import com.auctionsystem.chat.dto.CrearConversacionResponse;
import com.auctionsystem.chat.dto.MensajeChatDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerificacionChatService {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final VerificacionChatConversacionRepository conversacionRepository;
    private final VerificacionChatMensajeRepository mensajeRepository;

    @Transactional
    public CrearConversacionResponse crearConversacionDuenio(String email) {
        UsuarioAuth duenio = getUsuarioByEmail(email);
        ensureTieneRol(duenio, "DUENIO");

        VerificacionChatConversacion conversacion = new VerificacionChatConversacion();
        conversacion.setDuenioUsuario(duenio);
        conversacion.setEstado("ABIERTA");
        conversacion.setCreatedAt(LocalDateTime.now());
        conversacion.setUpdatedAt(LocalDateTime.now());
        conversacion = conversacionRepository.save(conversacion);
        return new CrearConversacionResponse(conversacion.getId(), conversacion.getEstado());
    }

    @Transactional
    public ConversacionDto tomarConversacion(Long conversacionId, String email) {
        UsuarioAuth empleado = getUsuarioByEmail(email);
        ensureTieneAlgunRol(empleado, Set.of("ADMIN", "EMPLEADO"));

        VerificacionChatConversacion conversacion = getConversacion(conversacionId);
        if (conversacion.getEmpleadoUsuario() == null) {
            conversacion.setEmpleadoUsuario(empleado);
        }
        conversacion.setUpdatedAt(LocalDateTime.now());
        conversacion = conversacionRepository.save(conversacion);
        return toConversacionDto(conversacion);
    }

    @Transactional
    public MensajeChatDto enviarMensaje(Long conversacionId, String email, String texto, Authentication auth) {
        UsuarioAuth remitente = getUsuarioByEmail(email);
        VerificacionChatConversacion conversacion = getConversacion(conversacionId);

        if (!puedeAcceder(conversacion, remitente, auth)) {
            throw new IllegalArgumentException("No tenes permisos para escribir en esta conversacion");
        }

        VerificacionChatMensaje mensaje = new VerificacionChatMensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setRemitente(remitente);
        mensaje.setTexto(texto.trim());
        mensaje.setEnviadoAt(LocalDateTime.now());
        mensaje = mensajeRepository.save(mensaje);

        conversacion.setUpdatedAt(LocalDateTime.now());
        conversacionRepository.save(conversacion);

        return toMensajeDto(mensaje);
    }

    @Transactional(readOnly = true)
    public List<MensajeChatDto> listarMensajes(Long conversacionId, String email, Authentication auth) {
        UsuarioAuth usuario = getUsuarioByEmail(email);
        VerificacionChatConversacion conversacion = getConversacion(conversacionId);

        if (!puedeAcceder(conversacion, usuario, auth)) {
            throw new IllegalArgumentException("No tenes permisos para ver esta conversacion");
        }

        return mensajeRepository.findByConversacionIdOrderByEnviadoAtAsc(conversacionId)
                .stream()
                .map(this::toMensajeDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversacionDto> listarConversaciones(String email, Authentication auth) {
        UsuarioAuth usuario = getUsuarioByEmail(email);

        if (hasRole(auth, "ROLE_DUENIO")) {
            return conversacionRepository.findByDuenioUsuarioIdOrderByUpdatedAtDesc(usuario.getId())
                    .stream()
                    .map(this::toConversacionDto)
                    .toList();
        }

        if (hasRole(auth, "ROLE_ADMIN") || hasRole(auth, "ROLE_EMPLEADO")) {
            return conversacionRepository.findByEstadoOrderByUpdatedAtDesc("ABIERTA")
                    .stream()
                    .map(this::toConversacionDto)
                    .toList();
        }

        throw new IllegalArgumentException("Rol no habilitado para listar conversaciones");
    }

    private UsuarioAuth getUsuarioByEmail(String email) {
        return usuarioAuthRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private VerificacionChatConversacion getConversacion(Long conversacionId) {
        return conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new IllegalArgumentException("Conversacion no encontrada"));
    }

    private boolean puedeAcceder(VerificacionChatConversacion c, UsuarioAuth user, Authentication auth) {
        boolean esDuenio = c.getDuenioUsuario().getId().equals(user.getId());
        boolean esEmpleadoAsignado = c.getEmpleadoUsuario() != null && c.getEmpleadoUsuario().getId().equals(user.getId());
        boolean esAdmin = hasRole(auth, "ROLE_ADMIN");
        return esDuenio || esEmpleadoAsignado || esAdmin;
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private void ensureTieneRol(UsuarioAuth usuario, String roleName) {
        boolean has = usuario.getRoles().stream().anyMatch(r -> roleName.equalsIgnoreCase(r.getNombre()));
        if (!has) {
            throw new IllegalArgumentException("El usuario no posee rol " + roleName);
        }
    }

    private void ensureTieneAlgunRol(UsuarioAuth usuario, Set<String> roles) {
        boolean has = usuario.getRoles().stream().anyMatch(r -> roles.contains(r.getNombre().toUpperCase()));
        if (!has) {
            throw new IllegalArgumentException("El usuario no posee rol habilitado");
        }
    }

    private ConversacionDto toConversacionDto(VerificacionChatConversacion c) {
        return new ConversacionDto(
                c.getId(),
                c.getDuenioUsuario().getId(),
                c.getEmpleadoUsuario() != null ? c.getEmpleadoUsuario().getId() : null,
                c.getEstado(),
                c.getUpdatedAt()
        );
    }

    private MensajeChatDto toMensajeDto(VerificacionChatMensaje m) {
        return new MensajeChatDto(
                m.getId(),
                m.getConversacion().getId(),
                m.getRemitente().getId(),
                m.getRemitente().getEmail(),
                m.getTexto(),
                m.getEnviadoAt()
        );
    }
}
