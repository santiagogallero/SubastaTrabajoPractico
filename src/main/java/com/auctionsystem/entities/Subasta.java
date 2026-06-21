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
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subastas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subasta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador", nullable = false)
    private Integer id;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @Column(name = "estado", length = 10)
    private String estado;

    @ManyToOne
    @JoinColumn(name = "subastador")
    private Subastador subastador;

    @Column(name = "ubicacion", length = 350)
    private String ubicacion;

    @Column(name = "capacidadAsistentes")
    private Integer capacidadAsistentes;

    @Column(name = "tieneDeposito", length = 2)
    private String tieneDeposito;

    @Column(name = "seguridadPropia", length = 2)
    private String seguridadPropia;

    @Column(name = "categoria", length = 10)
    private String categoria;

    @Column(name = "streamingUrl", length = 500)
    private String streamingUrl;

    @Column(name = "depositoNombre", length = 200)
    private String depositoNombre;

    @Column(name = "depositoDireccion", length = 350)
    private String depositoDireccion;
}
