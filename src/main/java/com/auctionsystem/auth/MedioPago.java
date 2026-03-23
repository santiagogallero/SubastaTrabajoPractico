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
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "medio_pago")
@Getter
@Setter
public class MedioPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioAuth usuario;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "alias_descripcion", nullable = false, length = 120)
    private String aliasDescripcion;

    @Column(name = "moneda", nullable = false, length = 10)
    private String moneda;

    @Column(name = "monto_garantia", precision = 18, scale = 2)
    private BigDecimal montoGarantia;

    @Column(name = "verificado", nullable = false)
    private Boolean verificado;

    @Column(name = "activo", nullable = false)
    private Boolean activo;
}
