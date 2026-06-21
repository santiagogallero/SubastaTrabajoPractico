package com.auctionsystem.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agrupacion de piezas de un mismo dueno para una subasta dedicada (consigna §11).
 */
@Entity
@Table(name = "colecciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coleccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador", nullable = false)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "duenio", nullable = false)
    private Duenio duenio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subasta", nullable = false)
    private Subasta subasta;

    @Column(name = "fechaCreacion")
    private LocalDateTime fechaCreacion;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "coleccion_productos",
            joinColumns = @JoinColumn(name = "coleccion"),
            inverseJoinColumns = @JoinColumn(name = "producto")
    )
    private Set<Producto> productos = new HashSet<>();
}
