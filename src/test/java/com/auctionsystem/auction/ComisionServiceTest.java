package com.auctionsystem.auction;

import com.auctionsystem.auction.dto.ComisionResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ComisionServiceTest {

    private final ComisionService service = new ComisionService(
            new BigDecimal("10"), new BigDecimal("8")
    );

    @Test
    void calcular_conImporteNormal() {
        ComisionResponse r = service.calcular(new BigDecimal("100"));

        assertThat(r.importeFinal()).isEqualByComparingTo("100.00");
        assertThat(r.comisionComprador()).isEqualByComparingTo("10.00");
        assertThat(r.comisionVendedor()).isEqualByComparingTo("8.00");
        assertThat(r.totalPagaComprador()).isEqualByComparingTo("110.00");
        assertThat(r.netoRecibeVendedor()).isEqualByComparingTo("92.00");
        assertThat(r.ingresoCasaSubasta()).isEqualByComparingTo("18.00");
    }

    @Test
    void calcular_conImporteCero() {
        ComisionResponse r = service.calcular(BigDecimal.ZERO);

        assertThat(r.comisionComprador()).isEqualByComparingTo("0.00");
        assertThat(r.comisionVendedor()).isEqualByComparingTo("0.00");
        assertThat(r.totalPagaComprador()).isEqualByComparingTo("0.00");
        assertThat(r.netoRecibeVendedor()).isEqualByComparingTo("0.00");
        assertThat(r.ingresoCasaSubasta()).isEqualByComparingTo("0.00");
    }

    @Test
    void calcular_conDecimales() {
        // 10% de 333.33 = 33.333 → 33.33
        // 8%  de 333.33 = 26.6664 → 26.67
        ComisionResponse r = service.calcular(new BigDecimal("333.33"));

        assertThat(r.comisionComprador()).isEqualByComparingTo("33.33");
        assertThat(r.comisionVendedor()).isEqualByComparingTo("26.67");
        assertThat(r.totalPagaComprador()).isEqualByComparingTo("366.66");
        assertThat(r.netoRecibeVendedor()).isEqualByComparingTo("306.66");
        assertThat(r.ingresoCasaSubasta()).isEqualByComparingTo("60.00");
    }

    @Test
    void calcular_conPorcentajesCustom() {
        ComisionService customService = new ComisionService(
                new BigDecimal("5"), new BigDecimal("3")
        );
        ComisionResponse r = customService.calcular(new BigDecimal("200"));

        assertThat(r.comisionComprador()).isEqualByComparingTo("10.00");
        assertThat(r.comisionVendedor()).isEqualByComparingTo("6.00");
        assertThat(r.totalPagaComprador()).isEqualByComparingTo("210.00");
        assertThat(r.netoRecibeVendedor()).isEqualByComparingTo("194.00");
        assertThat(r.ingresoCasaSubasta()).isEqualByComparingTo("16.00");
    }
}
