package com.api.v1.inventory.service;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.enumerate.ReportPeriod;
import com.api.v1.inventory.model.response.DashboardSummaryResponse;
import com.api.v1.inventory.model.response.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface DashboardService {

    DashboardSummaryResponse getDashboardSummary();

    DashboardSummaryResponse getDashboardSummary(ReportPeriod period, Instant fromDate, Instant toDate, String zoneId);

    PageResponse<ProductResponse> getLowStockProducts(Pageable pageable);
}
