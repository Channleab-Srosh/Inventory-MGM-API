package com.api.v1.inventory.common.util;

import com.api.v1.inventory.common.exception.BadRequestException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class IdListParser {

    public static final int MAX_IDS = 100;

    private IdListParser() {
    }

    /**
     * Parses a comma-separated ID string into a list of unique positive Long IDs.
     * Skips empty entries (e.g. "1,2,,3").
     * Enforces a maximum of 100 unique IDs.
     * Throws {@link BadRequestException} (HTTP 400) if any entry is non-numeric, zero, negative,
     * or if the result is empty or exceeds 100 IDs.
     */
    public static List<Long> parse(String rawIds) {
        if (rawIds == null || rawIds.trim().isEmpty()) {
            throw new BadRequestException("ID parameter must not be empty");
        }

        String[] tokens = rawIds.split(",");
        Set<Long> uniqueIds = new LinkedHashSet<>();

        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            long id;
            try {
                id = Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid ID value '" + trimmed + "': IDs must be positive numbers");
            }

            if (id <= 0) {
                throw new BadRequestException("Invalid ID value '" + id + "': IDs must be greater than zero");
            }

            uniqueIds.add(id);
        }

        if (uniqueIds.isEmpty()) {
            throw new BadRequestException("No valid IDs provided");
        }

        if (uniqueIds.size() > MAX_IDS) {
            throw new BadRequestException("Cannot process more than " + MAX_IDS + " IDs in a single request. Provided: " + uniqueIds.size());
        }

        return new ArrayList<>(uniqueIds);
    }
}
