package com.api.v1.inventory.service;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.request.CreateCustomerRequest;
import com.api.v1.inventory.model.request.UpdateCustomerRequest;
import com.api.v1.inventory.model.request.UpdateCustomerStatusRequest;
import com.api.v1.inventory.model.response.CustomerResponse;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerResponse createCustomer(CreateCustomerRequest request);

    PageResponse<CustomerResponse> getAllCustomers(String search, Pageable pageable);

    CustomerResponse getCustomerById(Long id);

    CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request);

    CustomerResponse updateCustomerStatus(Long id, UpdateCustomerStatusRequest request);

    void deleteCustomer(Long id);

    void deleteCustomers(java.util.List<Long> ids);
}
