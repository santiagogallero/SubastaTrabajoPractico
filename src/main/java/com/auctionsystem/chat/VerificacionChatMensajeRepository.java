package com.auctionsystem.chat;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificacionChatMensajeRepository extends JpaRepository<VerificacionChatMensaje, Long> {
    List<VerificacionChatMensaje> findByConversacionIdOrderByEnviadoAtAsc(Long conversacionId);
}
