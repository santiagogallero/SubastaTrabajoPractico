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
@Table(name = "duenios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Duenio {

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

    @Column(name = "verificacionFinanciera", length = 2)
    private String verificacionFinanciera;

    @Column(name = "verificacionJudicial", length = 2)
    private String verificacionJudicial;

    @Column(name = "calificacionRiesgo")
    private Integer calificacionRiesgo;

    @ManyToOne
    @JoinColumn(name = "verificador")
    private Empleado verificador;
}
