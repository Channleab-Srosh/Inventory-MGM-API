package com.api.v1.inventory.service;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.response.DashboardSummaryResponse;
import com.api.v1.inventory.model.response.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface DashboardService {

    DashboardSummaryResponse getDashboardSummary();

    PageResponse<ProductResponse> getLowStockProducts(Pageable pageable);
}
