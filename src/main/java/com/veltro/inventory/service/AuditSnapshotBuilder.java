package com.veltro.inventory.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generic utility for building forensic-audit snapshot maps (B3-03).
 *
 * <p>Replaces the 3 nearly-identical {@code buildXxxSnapshot()} methods previously
 * duplicated across {@code SaleService}, {@code PurchaseOrderService}, and
 * {@code InventoryService}.
 *
 * <p>Usage:
 * <pre>
 *   Map&lt;String, Object&gt; snap = AuditSnapshotBuilder.create()
 *       .put("id", entity.getId())
 *       .put("status", entity.getStatus() != null ? entity.getStatus().name() : null)
 *       .withDetails(entity.getActiveDetails(), d -&gt; AuditSnapshotBuilder.create()
 *           .put("id", d.getId())
 *           .build())
 *       .build();
 * </pre>
 */
public final class AuditSnapshotBuilder {

    private final Map<String, Object> snapshot = new LinkedHashMap<>();

    private AuditSnapshotBuilder() {
    }

    /** Creates a new builder. */
    public static AuditSnapshotBuilder create() {
        return new AuditSnapshotBuilder();
    }

    /**
     * Adds a key/value pair to the snapshot.
     *
     * @param key   the field name
     * @param value the value (null-safe)
     * @return this builder
     */
    public AuditSnapshotBuilder put(String key, Object value) {
        snapshot.put(key, value);
        return this;
    }

    /**
     * Adds an enum value as its {@code .name()} string, or null.
     *
     * @param key   the field name
     * @param value the enum value (may be null)
     * @return this builder
     */
    public AuditSnapshotBuilder putEnum(String key, Enum<?> value) {
        snapshot.put(key, value != null ? value.name() : null);
        return this;
    }

    /**
     * Adds a temporal value as its {@code .toString()} string, or null.
     *
     * @param key   the field name
     * @param value the temporal value (may be null)
     * @return this builder
     */
    public AuditSnapshotBuilder putTemporal(String key, Object value) {
        snapshot.put(key, value != null ? value.toString() : null);
        return this;
    }

    /**
     * Maps a filtered list of detail entities to snapshot maps and adds them under
     * the {@code "details"} key.
     *
     * @param details       the detail entities (already filtered for active)
     * @param detailMapper  function that converts each detail to a {@code Map<String, Object>}
     * @param <D>           the detail entity type
     * @return this builder
     */
    public <D> AuditSnapshotBuilder withDetails(List<D> details, Function<D, Map<String, Object>> detailMapper) {
        List<Map<String, Object>> detailMaps = details.stream()
                .map(detailMapper)
                .collect(Collectors.toList());
        snapshot.put("details", detailMaps);
        return this;
    }

    /** @return the immutable snapshot map */
    public Map<String, Object> build() {
        return snapshot;
    }
}
