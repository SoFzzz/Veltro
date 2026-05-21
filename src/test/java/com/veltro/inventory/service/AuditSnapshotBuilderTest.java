package com.veltro.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSnapshotBuilderTest {

    @Test
    @DisplayName("create — devuelve nueva instancia de builder")
    void create_returnsNewBuilder() {
        AuditSnapshotBuilder builder = AuditSnapshotBuilder.create();
        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("put — agrega par clave/valor al snapshot")
    void put_addsKeyValue() {
        Map<String, Object> snapshot = AuditSnapshotBuilder.create()
                .put("name", "Producto A")
                .build();

        assertThat(snapshot).containsEntry("name", "Producto A");
    }

    @Test
    @DisplayName("putEnum — agrega el nombre del enum")
    void putEnum_addsEnumName() {
        Map<String, Object> snapshot = AuditSnapshotBuilder.create()
                .putEnum("status", Thread.State.RUNNABLE)
                .build();

        assertThat(snapshot).containsEntry("status", "RUNNABLE");
    }

    @Test
    @DisplayName("putEnum — enum null agrega null")
    void putEnum_nullEnum_addsNull() {
        Map<String, Object> snapshot = AuditSnapshotBuilder.create()
                .putEnum("status", null)
                .build();

        assertThat(snapshot).containsEntry("status", null);
    }

    @Test
    @DisplayName("putTemporal — agrega toString del valor")
    void putTemporal_addsToString() {
        LocalDate date = LocalDate.of(2026, 5, 20);
        Map<String, Object> snapshot = AuditSnapshotBuilder.create()
                .putTemporal("date", date)
                .build();

        assertThat(snapshot).containsEntry("date", "2026-05-20");
    }

    @Test
    @DisplayName("putTemporal — null agrega null")
    void putTemporal_null_addsNull() {
        Map<String, Object> snapshot = AuditSnapshotBuilder.create()
                .putTemporal("date", null)
                .build();

        assertThat(snapshot).containsEntry("date", null);
    }

    @Test
    @DisplayName("withDetails — mapea lista de detalles correctamente")
    void withDetails_mapsDetailsList() {
        List<String> details = List.of("item1", "item2");
        Map<String, Object> snapshot = AuditSnapshotBuilder.create()
                .withDetails(details, d -> Map.of("value", d))
                .build();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detailMaps = (List<Map<String, Object>>) snapshot.get("details");
        assertThat(detailMaps).hasSize(2);
        assertThat(detailMaps.get(0)).containsEntry("value", "item1");
        assertThat(detailMaps.get(1)).containsEntry("value", "item2");
    }

    @Test
    @DisplayName("build — devuelve mapa con todas las entradas")
    void build_returnsMapWithAllEntries() {
        Map<String, Object> snapshot = AuditSnapshotBuilder.create()
                .put("id", 1L)
                .put("name", "Test")
                .put("price", 99.99)
                .build();

        assertThat(snapshot)
                .hasSize(3)
                .containsEntry("id", 1L)
                .containsEntry("name", "Test")
                .containsEntry("price", 99.99);
    }

    @Test
    @DisplayName("encadenamiento múltiple de puts funciona correctamente")
    void chainingMultiplePuts() {
        Map<String, Object> snapshot = AuditSnapshotBuilder.create()
                .put("id", 1L)
                .putEnum("role", Thread.State.NEW)
                .putTemporal("created", Instant.ofEpochMilli(0))
                .put("active", true)
                .build();

        assertThat(snapshot).hasSize(4);
        assertThat(snapshot.get("role")).isEqualTo("NEW");
        assertThat(snapshot.get("active")).isEqualTo(true);
    }

    @Test
    @DisplayName("withDetails — lista vacía produce lista vacía")
    void withDetails_emptyList() {
        Map<String, Object> snapshot = AuditSnapshotBuilder.create()
                .withDetails(List.of(), d -> Map.of())
                .build();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detailMaps = (List<Map<String, Object>>) snapshot.get("details");
        assertThat(detailMaps).isEmpty();
    }
}
