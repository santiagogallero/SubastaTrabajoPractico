package com.auctionsystem.chat;

import com.auctionsystem.chat.dto.ConversacionDto;
import com.auctionsystem.chat.dto.CrearConversacionResponse;
import com.auctionsystem.chat.dto.EnviarMensajeRequest;
import com.auctionsystem.chat.dto.MensajeChatDto;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verificacion-chat")
@RequiredArgsConstructor
public class VerificacionChatController {

    private final VerificacionChatService chatService;

    @PostMapping("/conversaciones")
    @PreAuthorize("hasRole('DUENIO')")
    public ResponseEntity<CrearConversacionResponse> crearConversacion(Principal principal) {
        return ResponseEntity.ok(chatService.crearConversacionDuenio(principal.getName()));
    }

    @PostMapping("/conversaciones/{conversacionId}/tomar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<ConversacionDto> tomarConversacion(@PathVariable Long conversacionId, Principal principal) {
        return ResponseEntity.ok(chatService.tomarConversacion(conversacionId, principal.getName()));
    }

    @GetMapping("/conversaciones")
    @PreAuthorize("hasAnyRole('DUENIO','ADMIN','EMPLEADO')")
    public ResponseEntity<List<ConversacionDto>> listarConversaciones(Principal principal, Authentication auth) {
        return ResponseEntity.ok(chatService.listarConversaciones(principal.getName(), auth));
    }

    @GetMapping("/conversaciones/{conversacionId}/mensajes")
    @PreAuthorize("hasAnyRole('DUENIO','ADMIN','EMPLEADO')")
    public ResponseEntity<List<MensajeChatDto>> listarMensajes(
            @PathVariable Long conversacionId,
            Principal principal,
            Authentication auth
    ) {
        return ResponseEntity.ok(chatService.listarMensajes(conversacionId, principal.getName(), auth));
    }

    @PostMapping("/conversaciones/{conversacionId}/mensajes")
    @PreAuthorize("hasAnyRole('DUENIO','ADMIN','EMPLEADO')")
    public ResponseEntity<MensajeChatDto> enviarMensaje(
            @PathVariable Long conversacionId,
            @Valid @RequestBody EnviarMensajeRequest request,
            Principal principal,
            Authentication auth
    ) {
        return ResponseEntity.ok(chatService.enviarMensaje(conversacionId, principal.getName(), request.texto(), auth));
    }
}
