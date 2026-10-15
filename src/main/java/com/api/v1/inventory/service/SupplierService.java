package com.api.v1.inventory.service;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.request.CreateSupplierRequest;
import com.api.v1.inventory.model.request.UpdateSupplierRequest;
import com.api.v1.inventory.model.response.SupplierResponse;
import org.springframework.data.domain.Pageable;

public interface SupplierService {

    SupplierResponse createSupplier(CreateSupplierRequest request);

    PageResponse<SupplierResponse> getAllSuppliers(String search, Pageable pageable);

    SupplierResponse getSupplierById(Long id);

    SupplierResponse updateSupplier(Long id, UpdateSupplierRequest request);

    void deleteSupplier(Long id);
}
