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
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @Column(name = "identificador", nullable = false)
    private Integer id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "identificador")
    private Persona persona;

    @ManyToOne
    @JoinColumn(name = "numeroPais")
    private Pais pais;

    @Column(name = "admitido", length = 2)
    private String admitido;

    @Column(name = "categoria", length = 10)
    private String categoria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "verificador", nullable = false)
    private Empleado verificador;
}
