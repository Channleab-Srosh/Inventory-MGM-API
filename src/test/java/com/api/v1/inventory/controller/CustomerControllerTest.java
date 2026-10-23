package com.api.v1.inventory.controller;

import com.api.v1.inventory.model.entity.Customer;
import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setupTokens() throws Exception {
        adminToken = obtainToken("admin", "Admin@12345");
        staffToken = obtainToken("staff", "Staff@12345");
    }

    private String obtainToken(String username, String password) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username(username)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} - Admin deletes single customer successfully")
    void testAdminDeletesSingleCustomer() throws Exception {
        long ts = System.currentTimeMillis();
        Customer customer = customerRepository.save(Customer.builder()
                .name("DeleteCust_" + ts)
                .phone("0811" + (ts % 100000))
                .build());

        mockMvc.perform(delete("/api/v1/customers/" + customer.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Customer deleted successfully")));

        mockMvc.perform(get("/api/v1/customers/" + customer.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{ids} - Bulk delete multiple customers successfully")
    void testAdminDeletesMultipleCustomersSuccessfully() throws Exception {
        long ts = System.currentTimeMillis();
        Customer c1 = customerRepository.save(Customer.builder().name("BulkCust1_" + ts).phone("0821" + (ts % 100000)).build());
        Customer c2 = customerRepository.save(Customer.builder().name("BulkCust2_" + ts).phone("0822" + (ts % 100000)).build());
        Customer c3 = customerRepository.save(Customer.builder().name("BulkCust3_" + ts).phone("0823" + (ts % 100000)).build());

        mockMvc.perform(delete("/api/v1/customers/" + c1.getId() + "," + c2.getId() + "," + c3.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Customers deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(customerRepository.existsById(c1.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(customerRepository.existsById(c2.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(customerRepository.existsById(c3.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{ids} - Empty entries like '1,2,,3' are skipped")
    void testBulkDeleteSkipsEmptyEntries() throws Exception {
        long ts = System.currentTimeMillis();
        Customer c1 = customerRepository.save(Customer.builder().name("SkipEmptyCust1_" + ts).phone("0831" + (ts % 100000)).build());
        Customer c2 = customerRepository.save(Customer.builder().name("SkipEmptyCust2_" + ts).phone("0832" + (ts % 100000)).build());

        mockMvc.perform(delete("/api/v1/customers/" + c1.getId() + ",," + c2.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Customers deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(customerRepository.existsById(c1.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(customerRepository.existsById(c2.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{ids} - Duplicate IDs are handled cleanly")
    void testBulkDeleteHandlesDuplicateIds() throws Exception {
        long ts = System.currentTimeMillis();
        Customer c1 = customerRepository.save(Customer.builder().name("DupCust1_" + ts).phone("0841" + (ts % 100000)).build());
        Customer c2 = customerRepository.save(Customer.builder().name("DupCust2_" + ts).phone("0842" + (ts % 100000)).build());

        mockMvc.perform(delete("/api/v1/customers/" + c1.getId() + "," + c1.getId() + "," + c2.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Customers deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(customerRepository.existsById(c1.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(customerRepository.existsById(c2.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{ids} - Non-numeric ID returns 400")
    void testBulkDeleteNonNumericReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/abc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("positive numbers")));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{ids} - Zero or negative ID returns 400")
    void testBulkDeleteZeroOrNegativeReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("greater than zero")));

        mockMvc.perform(delete("/api/v1/customers/-1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("greater than zero")));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{ids} - Only commas returns 400")
    void testBulkDeleteOnlyCommasReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/,")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("No valid IDs provided")));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{ids} - More than 100 IDs returns 400")
    void testBulkDeleteMoreThan100IdsReturns400() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 101; i++) {
            if (i > 1) sb.append(",");
            sb.append(i);
        }

        mockMvc.perform(delete("/api/v1/customers/" + sb)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Cannot process more than 100 IDs")));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{ids} - One missing ID returns 404 and deletes nothing")
    void testBulkDeleteOneMissingIdReturns404AndDeletesNothing() throws Exception {
        long ts = System.currentTimeMillis();
        Customer existing = customerRepository.save(Customer.builder().name("ExistingCust_" + ts).phone("0851" + (ts % 100000)).build());

        mockMvc.perform(delete("/api/v1/customers/" + existing.getId() + ",999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Customer not found with ID: 999999")))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));

        org.junit.jupiter.api.Assertions.assertTrue(customerRepository.existsById(existing.getId()), "Existing customer must NOT be deleted");
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{ids} - Several missing IDs returns 404 listing all and deletes nothing")
    void testBulkDeleteSeveralMissingIdsReturns404ListingAll() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/999991,999992")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Customers not found with IDs: [999991, 999992]")))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} - Staff cannot delete customer (requires ADMIN)")
    void testStaffCannotDeleteCustomer() throws Exception {
        long ts = System.currentTimeMillis();
        Customer customer = customerRepository.save(Customer.builder()
                .name("StaffNoDeleteCust_" + ts)
                .phone("0861" + (ts % 100000))
                .build());

        mockMvc.perform(delete("/api/v1/customers/" + customer.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} - Unauthenticated request returns 401")
    void testUnauthenticatedDeleteCustomerReturns401() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} - Nonexistent customer returns 404")
    void testDeleteNonExistentCustomerReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));
    }
}
