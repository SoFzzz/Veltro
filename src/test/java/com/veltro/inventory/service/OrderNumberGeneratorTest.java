package com.veltro.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderNumberGeneratorTest {

    @Test
    @DisplayName("generate — prefijo VLT formatea correctamente")
    void generate_withVLTPrefix_formatsCorrectly() {
        String result = OrderNumberGenerator.generate("VLT", 42L);
        int year = LocalDateTime.now().getYear();

        assertThat(result).isEqualTo("VLT-" + year + "-000042");
    }

    @Test
    @DisplayName("generate — prefijo PO formatea correctamente")
    void generate_withPOPrefix_formatsCorrectly() {
        String result = OrderNumberGenerator.generate("PO", 1L);
        int year = LocalDateTime.now().getYear();

        assertThat(result).isEqualTo("PO-" + year + "-000001");
    }

    @Test
    @DisplayName("generate — rellena secuencia a 6 dígitos")
    void generate_padsSequenceToSixDigits() {
        String result = OrderNumberGenerator.generate("VLT", 5L);

        assertThat(result).contains("-000005");
    }

    @Test
    @DisplayName("generate — secuencia grande no se trunca")
    void generate_handlesLargeSequenceValue() {
        String result = OrderNumberGenerator.generate("VLT", 1234567L);

        assertThat(result).contains("-1234567");
    }

    @Test
    @DisplayName("generate — contiene el año actual")
    void generate_containsCurrentYear() {
        String result = OrderNumberGenerator.generate("X", 1L);
        int year = LocalDateTime.now().getYear();

        assertThat(result).contains(String.valueOf(year));
    }

    @Test
    @DisplayName("generate — secuencia cero")
    void generate_zeroSequence() {
        String result = OrderNumberGenerator.generate("VLT", 0L);
        int year = LocalDateTime.now().getYear();

        assertThat(result).isEqualTo("VLT-" + year + "-000000");
    }
}
