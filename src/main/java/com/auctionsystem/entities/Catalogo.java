package com.auctionsystem.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalogos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Catalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador", nullable = false)
    private Integer id;

    @Column(name = "descripcion", nullable = false, length = 250)
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "subasta")
    private Subasta subasta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "responsable", nullable = false)
    private Empleado responsable;
}
