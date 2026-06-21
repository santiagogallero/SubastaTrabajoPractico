package com.auctionsystem.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seguros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seguro {

    @Id
    @Column(name = "nroPoliza", nullable = false, length = 30)
    private String nroPoliza;

    @Column(name = "compania", nullable = false, length = 150)
    private String compania;

    @Column(name = "polizaCombinada", length = 2)
    private String polizaCombinada;

    @Column(name = "importe", nullable = false, precision = 18, scale = 2)
    private BigDecimal importe;

    @Column(name = "contactoTelefono", length = 40)
    private String contactoTelefono;

    @Column(name = "contactoEmail", length = 150)
    private String contactoEmail;
}
