package com.auctionsystem.auth;

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
@Table(name = "registro_postor")
@Getter
@Setter
public class RegistroPostor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioAuth usuario;

    @Column(name = "etapa", nullable = false, length = 30)
    private String etapa;

    @Column(name = "categoria", length = 30)
    private String categoria;

    @Column(name = "doc_frente_url", length = 300)
    private String docFrenteUrl;

    @Column(name = "doc_dorso_url", length = 300)
    private String docDorsoUrl;

    @Column(name = "domicilio_legal", length = 255)
    private String domicilioLegal;

    @Column(name = "pais_origen", length = 120)
    private String paisOrigen;

    @Column(name = "numero_tramite", length = 40)
    private String numeroTramite;

    @Column(name = "verificacion_externa_estado", length = 30)
    private String verificacionExternaEstado;

    @Column(name = "verificacion_externa_fuente", length = 60)
    private String verificacionExternaFuente;

    @Column(name = "verificacion_externa_detalle", length = 400)
    private String verificacionExternaDetalle;

    @Column(name = "verificado_por", length = 120)
    private String verificadoPor;

    @Column(name = "verificado_at")
    private LocalDateTime verificadoAt;

    @Column(name = "motivo_rechazo", length = 400)
    private String motivoRechazo;
}
