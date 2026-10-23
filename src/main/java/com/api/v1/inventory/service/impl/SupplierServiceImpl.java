package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.exception.BadRequestException;
import com.api.v1.inventory.common.exception.ConflictException;
import com.api.v1.inventory.common.exception.ResourceNotFoundException;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.entity.Supplier;
import com.api.v1.inventory.model.request.CreateSupplierRequest;
import com.api.v1.inventory.model.request.UpdateSupplierRequest;
import com.api.v1.inventory.model.response.SupplierResponse;
import com.api.v1.inventory.repository.ProductRepository;
import com.api.v1.inventory.repository.SupplierRepository;
import com.api.v1.inventory.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request) {
        String trimmedName = request.getName().trim();
        String trimmedPhone = request.getPhone().trim();
        String trimmedEmail = request.getEmail() != null && !request.getEmail().trim().isEmpty()
                ? request.getEmail().trim()
                : null;
        String trimmedAddress = request.getAddress() != null && !request.getAddress().trim().isEmpty()
                ? request.getAddress().trim()
                : null;

        if (supplierRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException("Supplier with name '" + trimmedName + "' already exists");
        }

        if (supplierRepository.existsByPhone(trimmedPhone)) {
            throw new ConflictException("Supplier with phone '" + trimmedPhone + "' already exists");
        }

        if (trimmedEmail != null && supplierRepository.existsByEmailIgnoreCase(trimmedEmail)) {
            throw new ConflictException("Supplier with email '" + trimmedEmail + "' already exists");
        }

        Supplier supplier = Supplier.builder()
                .name(trimmedName)
                .phone(trimmedPhone)
                .email(trimmedEmail)
                .address(trimmedAddress)
                .build();

        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier created successfully with id: {}", saved.getId());
        return SupplierResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> getAllSuppliers(String search, Pageable pageable) {
        Page<Supplier> supplierPage;
        if (search != null && !search.trim().isEmpty()) {
            String term = search.trim();
            supplierPage = supplierRepository.findByNameContainingIgnoreCaseOrPhoneContaining(term, term, pageable);
        } else {
            supplierPage = supplierRepository.findAll(pageable);
        }
        return PageResponse.from(supplierPage.map(SupplierResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        return SupplierResponse.fromEntity(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(Long id, UpdateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        String trimmedName = request.getName().trim();
        String trimmedPhone = request.getPhone().trim();
        String trimmedEmail = request.getEmail() != null && !request.getEmail().trim().isEmpty()
                ? request.getEmail().trim()
                : null;
        String trimmedAddress = request.getAddress() != null && !request.getAddress().trim().isEmpty()
                ? request.getAddress().trim()
                : null;

        if (!supplier.getName().equalsIgnoreCase(trimmedName) &&
                supplierRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new ConflictException("Supplier with name '" + trimmedName + "' already exists");
        }

        if (!supplier.getPhone().equals(trimmedPhone) &&
                supplierRepository.existsByPhoneAndIdNot(trimmedPhone, id)) {
            throw new ConflictException("Supplier with phone '" + trimmedPhone + "' already exists");
        }

        if (trimmedEmail != null && (supplier.getEmail() == null || !supplier.getEmail().equalsIgnoreCase(trimmedEmail)) &&
                supplierRepository.existsByEmailIgnoreCaseAndIdNot(trimmedEmail, id)) {
            throw new ConflictException("Supplier with email '" + trimmedEmail + "' already exists");
        }

        supplier.setName(trimmedName);
        supplier.setPhone(trimmedPhone);
        supplier.setEmail(trimmedEmail);
        supplier.setAddress(trimmedAddress);

        Supplier updated = supplierRepository.save(supplier);
        log.info("Supplier updated successfully with id: {}", updated.getId());
        return SupplierResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteSupplier(Long id) {
        deleteSuppliers(List.of(id));
    }

    @Override
    @Transactional
    public void deleteSuppliers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("No supplier IDs provided for deletion");
        }

        // 1. Single query to load all requested suppliers
        List<Supplier> suppliers = supplierRepository.findAllById(ids);
        Set<Long> foundIds = suppliers.stream()
                .map(Supplier::getId)
                .collect(Collectors.toSet());

        // 2. Identify missing IDs and reject with 404 listing ALL missing IDs
        List<Long> missingIds = ids.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            String msg = (missingIds.size() == 1)
                    ? "Supplier not found with id: " + missingIds.get(0) + ". Nothing was deleted."
                    : "Suppliers not found with IDs: " + missingIds + ". Nothing was deleted.";
            throw new ResourceNotFoundException(msg);
        }

        // 3. Check for product references (409 Conflict)
        List<Long> referencedIds = productRepository.findReferencedSupplierIds(ids);
        if (!referencedIds.isEmpty()) {
            throw new ConflictException(
                    "Cannot delete supplier because it is currently referenced by one or more products. Referenced supplier IDs: "
                            + referencedIds + ". Nothing was deleted."
            );
        }

        // 4. Atomic delete
        supplierRepository.deleteAll(suppliers);
        log.info("Deleted {} supplier(s) with ids: {}", suppliers.size(), ids);
    }
}
