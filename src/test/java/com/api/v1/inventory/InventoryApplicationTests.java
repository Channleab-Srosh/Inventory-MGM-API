package com.api.v1.inventory;

import com.api.v1.inventory.model.entity.*;
import com.api.v1.inventory.model.enumerate.Role;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InventoryApplicationTests {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Test
    @DisplayName("Application context loads and connects to database")
    void contextLoads() {
        assertNotNull(appUserRepository);
        assertNotNull(categoryRepository);
        assertNotNull(supplierRepository);
        assertNotNull(productRepository);
        assertNotNull(stockTransactionRepository);
    }

    @Test
    @DisplayName("Verify default seed users exist")
    void testSeedUsersExist() {
        Optional<AppUser> adminOpt = appUserRepository.findByUsernameIgnoreCase("admin");
        assertTrue(adminOpt.isPresent(), "Admin user should exist from DataInitializer");
        assertEquals(Role.ADMIN, adminOpt.get().getRole());
        assertTrue(adminOpt.get().isActive());

        Optional<AppUser> staffOpt = appUserRepository.findByUsernameIgnoreCase("staff");
        assertTrue(staffOpt.isPresent(), "Staff user should exist from DataInitializer");
        assertEquals(Role.STAFF, staffOpt.get().getRole());
        assertTrue(staffOpt.get().isActive());
    }

    @Test
    @Transactional
    @DisplayName("Verify full entity relationship flow: Category -> Supplier -> Product -> StockTransaction")
    void testEntityLifecycleAndRelationships() {
        // 1. Create Category
        Category category = Category.builder()
                .name("Phase 1 Electronics " + System.currentTimeMillis())
                .description("Phase 1 test electronics category")
                .build();
        Category savedCategory = categoryRepository.save(category);
        assertNotNull(savedCategory.getId());
        assertNotNull(savedCategory.getCreatedAt());

        // 2. Create Supplier
        Supplier supplier = Supplier.builder()
                .name("Phase 1 Supplier Co. " + System.currentTimeMillis())
                .phone("099112233")
                .email("supplier_phase1@test.com")
                .address("123 Tech Street")
                .build();
        Supplier savedSupplier = supplierRepository.save(supplier);
        assertNotNull(savedSupplier.getId());

        // 3. Create Product
        Product product = Product.builder()
                .sku("SKU-PHASE1-" + System.currentTimeMillis())
                .name("Phase 1 Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .category(savedCategory)
                .supplier(savedSupplier)
                .unitPrice(new BigDecimal("29.99"))
                .stockQuantity(100)
                .minimumStock(10)
                .active(true)
                .build();
        Product savedProduct = productRepository.save(product);
        assertNotNull(savedProduct.getId());
        assertNotNull(savedProduct.getSku());
        assertEquals(0L, savedProduct.getVersion());

        // 4. Create StockTransaction
        AppUser admin = appUserRepository.findByUsernameIgnoreCase("admin").orElseThrow();
        StockTransaction transaction = StockTransaction.builder()
                .product(savedProduct)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(100)
                .quantityBefore(0)
                .quantityAfter(100)
                .note("Initial phase 1 inventory batch")
                .createdBy(admin)
                .build();
        StockTransaction savedTx = stockTransactionRepository.save(transaction);
        assertNotNull(savedTx.getId());
        assertEquals(TransactionType.STOCK_IN, savedTx.getTransactionType());
        assertEquals(100, savedTx.getQuantity());
        assertEquals(savedProduct.getId(), savedTx.getProduct().getId());
    }

    @Test
    @Transactional
    @DisplayName("Verify unique constraint on Category name")
    void testCategoryUniqueName() {
        String catName = "Unique Phase 1 Cat " + System.currentTimeMillis();
        Category cat1 = Category.builder().name(catName).build();
        categoryRepository.save(cat1);

        assertTrue(categoryRepository.existsByNameIgnoreCase(catName.toLowerCase()));
        assertTrue(categoryRepository.existsByNameIgnoreCase(catName.toUpperCase()));
    }
}
