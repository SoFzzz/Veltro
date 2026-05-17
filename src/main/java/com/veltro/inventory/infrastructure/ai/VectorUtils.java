package com.veltro.inventory.infrastructure.ai;

public final class VectorUtils {

    private VectorUtils() {
    }

    /**
     * Formats a float array into the pgvector-compatible string representation.
     * Example output: "[0.123,0.456,0.789]".
     *
     * @param embedding float array to format
     * @return pgvector-compatible string
     */
    public static String formatPgVector(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder(embedding.length * 10 + 2);
        builder.append("[");
        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) {
                builder.append(",");
            }
            builder.append(embedding[index]);
        }
        builder.append("]");
        return builder.toString();
    }
}
