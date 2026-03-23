package com.auctionsystem.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "empleados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empleado {

    @Id
    @Column(name = "identificador", nullable = false)
    private Integer id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "identificador")
    private Persona persona;

    @Column(name = "cargo", length = 100)
    private String cargo;

    @ManyToOne
    @JoinColumn(name = "sector")
    private Sector sector;
}
