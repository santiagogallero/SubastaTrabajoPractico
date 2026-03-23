package com.auctionsystem.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "itemsCatalogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador", nullable = false)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "catalogo", nullable = false)
    private Catalogo catalogo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto", nullable = false)
    private Producto producto;

    @Column(name = "precioBase", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "comision", nullable = false, precision = 18, scale = 2)
    private BigDecimal comision;

    @Column(name = "subastado", length = 2)
    private String subastado;
}
