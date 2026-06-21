package com.auctionsystem.payout;

import com.auctionsystem.entities.Duenio;
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
@Table(name = "cuenta_cobro")
@Getter
@Setter
public class CuentaCobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "duenio_id", nullable = false)
    private Duenio duenio;

    @Column(nullable = false, length = 120)
    private String alias;

    @Column(nullable = false, length = 10)
    private String moneda;

    @Column(nullable = false)
    private Boolean extranjera;

    @Column(nullable = false, length = 120)
    private String banco;

    @Column(name = "numero_cuenta", nullable = false, length = 64)
    private String numeroCuenta;

    @Column(name = "swift_code", length = 32)
    private String swiftCode;

    @Column(nullable = false, length = 120)
    private String titular;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
