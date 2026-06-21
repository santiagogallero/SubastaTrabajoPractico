package com.auctionsystem.compliance;

import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.entities.RegistroDeSubasta;
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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pago_subasta_ext")
@Getter
@Setter
public class PagoSubastaExt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registro_subasta_id", nullable = false)
    private RegistroDeSubasta registroSubasta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_auth_id", nullable = false)
    private UsuarioAuth usuario;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDateTime fechaVencimiento;

    @Column(name = "estado_pago", nullable = false, length = 20)
    private String estadoPago;

    @Column(name = "monto_ofertado", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoOfertado;

    @Column(name = "monto_multa", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoMulta;

    @Column(name = "multa_aplicada", nullable = false)
    private Boolean multaAplicada;

    @Column(name = "notificado_vencimiento", nullable = false)
    private Boolean notificadoVencimiento;

    @Column(name = "notificado_mora", nullable = false)
    private Boolean notificadoMora;

    @Column(name = "fecha_limite_regularizacion")
    private LocalDateTime fechaLimiteRegularizacion;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "monto_total", precision = 18, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "moneda", length = 10)
    private String moneda;

    @Column(name = "producto_descripcion", length = 255)
    private String productoDescripcion;

    @Column(name = "medio_pago_id")
    private Long medioPagoId;

    @Column(name = "transaccion_id", length = 64)
    private String transaccionId;
}
