package com.auctionsystem.auction;

import com.auctionsystem.auction.dto.ComisionResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ComisionService {

    private final BigDecimal porcentajeComprador;
    private final BigDecimal porcentajeVendedor;

    public ComisionService(
            @Value("${app.auction.commission.buyer-percent:10}") BigDecimal porcentajeComprador,
            @Value("${app.auction.commission.seller-percent:8}") BigDecimal porcentajeVendedor
    ) {
        this.porcentajeComprador = porcentajeComprador;
        this.porcentajeVendedor = porcentajeVendedor;
    }

    public ComisionResponse calcular(BigDecimal importeFinal) {
        BigDecimal base = importeFinal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal comisionComprador = percent(base, porcentajeComprador);
        BigDecimal comisionVendedor = percent(base, porcentajeVendedor);

        BigDecimal totalPagaComprador = base.add(comisionComprador).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netoRecibeVendedor = base.subtract(comisionVendedor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ingresoCasaSubasta = comisionComprador.add(comisionVendedor).setScale(2, RoundingMode.HALF_UP);

        return new ComisionResponse(
                base,
                porcentajeComprador,
                porcentajeVendedor,
                comisionComprador,
                comisionVendedor,
                totalPagaComprador,
                netoRecibeVendedor,
                ingresoCasaSubasta
        );
    }

    private BigDecimal percent(BigDecimal monto, BigDecimal porcentaje) {
        return monto
                .multiply(porcentaje)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
