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
@Table(name = "verificacion_chat_conversacion")
@Getter
@Setter
public class VerificacionChatConversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "duenio_usuario_id", nullable = false)
    private UsuarioAuth duenioUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_usuario_id")
    private UsuarioAuth empleadoUsuario;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
