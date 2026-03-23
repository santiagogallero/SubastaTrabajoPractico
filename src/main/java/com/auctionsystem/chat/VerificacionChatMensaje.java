package com.auctionsystem.chat;

import com.auctionsystem.auth.UsuarioAuth;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "verificacion_chat_mensaje")
@Getter
@Setter
public class VerificacionChatMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private VerificacionChatConversacion conversacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remitente_usuario_id", nullable = false)
    private UsuarioAuth remitente;

    @Column(name = "texto", nullable = false, length = 2000)
    private String texto;

    @Column(name = "enviado_at", nullable = false)
    private LocalDateTime enviadoAt;
}
