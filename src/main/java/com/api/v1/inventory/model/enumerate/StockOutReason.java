package com.api.v1.inventory.model.enumerate;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Reason for a STOCK_OUT transaction.
 */
public enum StockOutReason {
    SALE,
    DAMAGE,
    OTHER;

    @JsonCreator
    public static StockOutReason fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String upper = value.trim().toUpperCase();
        if ("DAMAGE".equals(upper) || "DAMAGED".equals(upper)) {
            return DAMAGE;
        }
        if ("SALE".equals(upper) || "SALES".equals(upper)) {
            return SALE;
        }
        if ("OTHER".equals(upper) || "OTH".equals(upper) || "OTHERS".equals(upper)) {
            return OTHER;
        }
        for (StockOutReason r : values()) {
            if (r.name().equalsIgnoreCase(upper)) {
                return r;
            }
        }
        return OTHER;
    }
}
