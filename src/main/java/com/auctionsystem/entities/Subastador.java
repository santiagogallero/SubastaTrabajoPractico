package com.auctionsystem.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subastadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subastador {

    @Id
    @Column(name = "identificador", nullable = false)
    private Integer id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "identificador")
    private Persona persona;

    @Column(name = "matricula", length = 15)
    private String matricula;

    @Column(name = "region", length = 50)
    private String region;
}
