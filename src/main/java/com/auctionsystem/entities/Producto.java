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

    @Builder.Default
    @Column(name = "descripcionCatalogo", length = 500)
    private String descripcionCatalogo = "No Posee";

    @Column(name = "descripcionCompleta", nullable = false, length = 300)
    private String descripcionCompleta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "revisor", nullable = false)
    private Empleado revisor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "duenio", nullable = false)
    private Duenio duenio;

    @ManyToOne
    @JoinColumn(name = "seguro", referencedColumnName = "nroPoliza")
    private Seguro seguro;
}
