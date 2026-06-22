package com.auctionsystem.chat;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificacionChatConversacionRepository extends JpaRepository<VerificacionChatConversacion, Long> {
    List<VerificacionChatConversacion> findByDuenioUsuarioIdOrderByUpdatedAtDesc(Long duenioUsuarioId);
    List<VerificacionChatConversacion> findByEstadoOrderByUpdatedAtDesc(String estado);
    java.util.Optional<VerificacionChatConversacion> findByProductoId(Integer productoId);
}
