package com.auctionsystem.compliance;

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
@Table(name = "bloqueo_participacion")
@Getter
@Setter
public class BloqueoParticipacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_auth_id", nullable = false)
    private UsuarioAuth usuario;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "motivo", nullable = false, length = 255)
    private String motivo;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
