package com.auctionsystem.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador", nullable = false)
    private Integer id;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "disponible", length = 2)
    private String disponible;

    @Column(name = "titulo", length = 200)
    private String titulo;

    @Column(name = "categoria", length = 100)
    private String categoria;

    @Builder.Default
    @Column(name = "descripcionCatalogo", length = 500)
    private String descripcionCatalogo = "No Posee";

    @Column(name = "descripcionCompleta", nullable = false, length = 1000)
    private String descripcionCompleta;

    /** Procedencia / historia / origen declarado por el dueno. */
    @Column(name = "historia", length = 2000)
    private String historia;

    /** El postor declara ser dueno legitimo del articulo. */
    @Builder.Default
    @Column(name = "declaraPropiedad", nullable = false)
    private boolean declaraPropiedad = false;

    /** El postor declara que el articulo tiene origen licito. */
    @Builder.Default
    @Column(name = "origenLicit", nullable = false)
    private boolean origenLicit = false;

    /** Estado del proceso de inspeccion: PENDIENTE, APROBADO o RECHAZADO. */
    @Builder.Default
    @Column(name = "estadoInspeccion", length = 20)
    private String estadoInspeccion = ESTADO_PENDIENTE;

    @Column(name = "motivoRechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "fechaInspeccion")
    private LocalDateTime fechaInspeccion;

    /** El revisor se asigna recien cuando un empleado inspecciona el articulo. */
    @ManyToOne
    @JoinColumn(name = "revisor")
    private Empleado revisor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "duenio", nullable = false)
    private Duenio duenio;

    @ManyToOne
    @JoinColumn(name = "seguro", referencedColumnName = "nroPoliza")
    private Seguro seguro;

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_APROBADO = "APROBADO";
    public static final String ESTADO_RECHAZADO = "RECHAZADO";
}
