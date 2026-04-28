package com.veltro.inventory.infrastructure.persistence;

import com.pgvector.PGvector;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

/**
 * JPA AttributeConverter for {@link PGvector} <-> PostgreSQL {@code vector} column.
 *
 * <p>Uses {@link PGobject} with type "vector" so that the JDBC driver sends the value
 * with the correct PostgreSQL type label, avoiding the
 * "expression is of type character varying" cast error.
 */
@Converter
public class PGvectorConverter implements AttributeConverter<PGvector, Object> {

    @Override
    public Object convertToDatabaseColumn(PGvector attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            PGobject pgObject = new PGobject();
            pgObject.setType("vector");
            pgObject.setValue(attribute.toString());
            return pgObject;
        } catch (SQLException e) {
            throw new IllegalArgumentException("Could not convert PGvector to PGobject", e);
        }
    }

    @Override
    public PGvector convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        String value;
        if (dbData instanceof PGobject pgObj) {
            value = pgObj.getValue();
        } else {
            value = dbData.toString();
        }
        if (value == null || value.isBlank()) {
            return null;
        }
        // Strip surrounding brackets and parse back to float[]
        String stripped = value.trim();
        if (stripped.startsWith("[")) {
            stripped = stripped.substring(1, stripped.length() - 1);
        }
        String[] parts = stripped.split(",");
        float[] values = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Float.parseFloat(parts[i].trim());
        }
        return new PGvector(values);
    }
}
