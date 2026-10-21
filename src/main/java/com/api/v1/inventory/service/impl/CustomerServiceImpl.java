package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.exception.ConflictException;
import com.api.v1.inventory.common.exception.ResourceNotFoundException;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.entity.Customer;
import com.api.v1.inventory.model.request.CreateCustomerRequest;
import com.api.v1.inventory.model.request.UpdateCustomerRequest;
import com.api.v1.inventory.model.request.UpdateCustomerStatusRequest;
import com.api.v1.inventory.model.response.CustomerResponse;
import com.api.v1.inventory.repository.CustomerRepository;
import com.api.v1.inventory.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        String trimmedName    = request.getName().trim();
        String trimmedPhone   = request.getPhone().trim();
        String trimmedEmail   = request.getEmail() != null && !request.getEmail().trim().isEmpty()
                ? request.getEmail().trim().toLowerCase() : null;
        String trimmedAddress = request.getAddress() != null && !request.getAddress().trim().isEmpty()
                ? request.getAddress().trim() : null;

        if (customerRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException("Customer with name '" + trimmedName + "' already exists");
        }
        if (customerRepository.existsByPhone(trimmedPhone)) {
            throw new ConflictException("Customer with phone '" + trimmedPhone + "' already exists");
        }
        if (trimmedEmail != null && customerRepository.existsByEmailIgnoreCase(trimmedEmail)) {
            throw new ConflictException("Customer with email '" + trimmedEmail + "' already exists");
        }

        Customer customer = Customer.builder()
                .name(trimmedName)
                .phone(trimmedPhone)
                .email(trimmedEmail)
                .address(trimmedAddress)
                .active(true)
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Customer created: id={}, name={}", saved.getId(), saved.getName());
        return CustomerResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(String search, Pageable pageable) {
        Page<Customer> page;
        if (search != null && !search.trim().isEmpty()) {
            String term = search.trim();
            page = customerRepository.findByNameContainingIgnoreCaseOrPhoneContaining(term, term, pageable);
        } else {
            page = customerRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(CustomerResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        return CustomerResponse.fromEntity(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));

        String trimmedName    = request.getName().trim();
        String trimmedPhone   = request.getPhone().trim();
        String trimmedEmail   = request.getEmail() != null && !request.getEmail().trim().isEmpty()
                ? request.getEmail().trim().toLowerCase() : null;
        String trimmedAddress = request.getAddress() != null && !request.getAddress().trim().isEmpty()
                ? request.getAddress().trim() : null;

        if (!customer.getName().equalsIgnoreCase(trimmedName) &&
                customerRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new ConflictException("Customer with name '" + trimmedName + "' already exists");
        }
        if (!customer.getPhone().equals(trimmedPhone) &&
                customerRepository.existsByPhoneAndIdNot(trimmedPhone, id)) {
            throw new ConflictException("Customer with phone '" + trimmedPhone + "' already exists");
        }
        if (trimmedEmail != null &&
                (customer.getEmail() == null || !customer.getEmail().equalsIgnoreCase(trimmedEmail)) &&
                customerRepository.existsByEmailIgnoreCaseAndIdNot(trimmedEmail, id)) {
            throw new ConflictException("Customer with email '" + trimmedEmail + "' already exists");
        }

        customer.setName(trimmedName);
        customer.setPhone(trimmedPhone);
        customer.setEmail(trimmedEmail);
        customer.setAddress(trimmedAddress);

        Customer updated = customerRepository.save(customer);
        log.info("Customer updated: id={}, name={}", updated.getId(), updated.getName());
        return CustomerResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomerStatus(Long id, UpdateCustomerStatusRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        customer.setActive(request.getActive());
        Customer updated = customerRepository.save(customer);
        log.info("Customer status updated: id={}, active={}", updated.getId(), updated.isActive());
        return CustomerResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        customerRepository.delete(customer);
        log.info("Customer deleted: id={}, name={}", id, customer.getName());
    }
}
