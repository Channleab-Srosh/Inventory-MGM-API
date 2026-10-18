package com.api.v1.inventory.common.pagination;

import com.api.v1.inventory.common.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Validates pagination inputs and enforces safe, deterministic sorting across the API.
 */
public final class PaginationValidator {

    public static final int MIN_PAGE_SIZE = 1;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_TIE_BREAKER = "id";

    private PaginationValidator() {
        // Utility class
    }

    /**
     * Validates page index, page size, and sort properties against an allowed whitelist,
     * and appends a deterministic tie-breaker (id) if not already present.
     *
     * @param pageable              the incoming Spring Pageable
     * @param allowedSortProperties allowed sort property names for the endpoint
     * @param tieBreakerProperty    property to break ties (typically "id")
     * @return a validated and stabilized Pageable
     * @throws BadRequestException if page < 0, size < 1, or sort property is not allowed
     */
    public static Pageable validateAndStabilize(
            Pageable pageable,
            Collection<String> allowedSortProperties,
            String tieBreakerProperty
    ) {
        if (pageable == null) {
            return PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, tieBreakerProperty));
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest req = servletAttributes.getRequest();
            String rawPage = req.getParameter("page");
            if (rawPage != null) {
                try {
                    int p = Integer.parseInt(rawPage);
                    if (p < 0) {
                        throw new BadRequestException("Page index must not be less than zero (0-indexed)");
                    }
                } catch (NumberFormatException ignored) {
                    // Type mismatch is handled by Spring/handler
                }
            }
            String rawSize = req.getParameter("size");
            if (rawSize != null) {
                try {
                    int s = Integer.parseInt(rawSize);
                    if (s < MIN_PAGE_SIZE) {
                        throw new BadRequestException("Page size must not be less than " + MIN_PAGE_SIZE);
                    }
                } catch (NumberFormatException ignored) {
                    // Type mismatch is handled by Spring/handler
                }
            }
        }

        if (pageable.getPageNumber() < 0) {
            throw new BadRequestException("Page index must not be less than zero (0-indexed)");
        }

        if (pageable.getPageSize() < MIN_PAGE_SIZE) {
            throw new BadRequestException("Page size must not be less than " + MIN_PAGE_SIZE);
        }

        Sort sort = pageable.getSort();
        List<Sort.Order> validatedOrders = new ArrayList<>();
        boolean containsTieBreaker = false;
        Sort.Direction primaryDirection = Sort.Direction.ASC;

        if (sort.isSorted()) {
            boolean first = true;
            for (Sort.Order order : sort) {
                String property = order.getProperty();
                if (allowedSortProperties != null && !allowedSortProperties.contains(property)) {
                    throw new BadRequestException(String.format(
                            "Invalid sort property '%s'. Allowed sort properties: %s",
                            property, allowedSortProperties
                    ));
                }

                if (first) {
                    primaryDirection = order.getDirection();
                    first = false;
                }

                if (property.equalsIgnoreCase(tieBreakerProperty)) {
                    containsTieBreaker = true;
                }
                validatedOrders.add(order);
            }
        }

        // Add deterministic tie-breaker if not already present in sort orders
        if (!containsTieBreaker && tieBreakerProperty != null) {
            validatedOrders.add(new Sort.Order(primaryDirection, tieBreakerProperty));
        }

        Sort finalSort = validatedOrders.isEmpty()
                ? Sort.by(Sort.Direction.ASC, tieBreakerProperty)
                : Sort.by(validatedOrders);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), finalSort);
    }

    public static Pageable validateAndStabilize(Pageable pageable, Collection<String> allowedSortProperties) {
        return validateAndStabilize(pageable, allowedSortProperties, DEFAULT_TIE_BREAKER);
    }
}
