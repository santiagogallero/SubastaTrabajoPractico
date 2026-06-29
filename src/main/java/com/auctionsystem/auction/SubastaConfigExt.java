package com.auctionsystem.auction;

import com.auctionsystem.entities.Subasta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subasta_config_ext")
@Getter
@Setter
public class SubastaConfigExt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subasta_id", nullable = false)
    private Subasta subasta;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_actual_id")
    private com.auctionsystem.entities.ItemCatalogo itemActual;

    @Column(name = "item_iniciado_at")
    private java.time.LocalDateTime itemIniciadoAt;

    @Column(name = "duracion_item_minutos", nullable = false)
    private Integer duracionItemMinutos = 5;
}
