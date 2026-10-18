package com.api.v1.inventory.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Swagger OpenAPI documents clean pagination parameters without request bodies on GET")
    void testOpenApiPaginationDocumentation() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode paths = root.path("paths");

        // Verify GET /api/v1/products
        JsonNode productsGet = paths.path("/api/v1/products").path("get");
        assertFalse(productsGet.isMissingNode(), "GET /api/v1/products must be documented");

        // Ensure NO request body on GET
        assertTrue(productsGet.path("requestBody").isMissingNode(), "GET /api/v1/products must not have a request body");

        JsonNode parameters = productsGet.path("parameters");
        assertTrue(parameters.isArray(), "Parameters must be an array");

        JsonNode pageParam = null;
        JsonNode sizeParam = null;
        JsonNode sortParam = null;

        for (JsonNode param : parameters) {
            String name = param.path("name").asText();
            if ("page".equals(name)) {
                pageParam = param;
            } else if ("size".equals(name)) {
                sizeParam = param;
            } else if ("sort".equals(name)) {
                sortParam = param;
            }
        }

        System.out.println("PRODUCTS_OPENAPI_PARAMETERS_JSON_START");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters));
        System.out.println("PRODUCTS_OPENAPI_PARAMETERS_JSON_END");

        // Verify 'page' parameter
        assertNotNull(pageParam, "page query parameter must be present");
        assertEquals("query", pageParam.path("in").asText());
        assertEquals("integer", pageParam.path("schema").path("type").asText());
        assertEquals("0", pageParam.path("schema").path("default").asText());
        assertEquals(0, pageParam.path("schema").path("minimum").asInt());

        // Verify 'size' parameter
        assertNotNull(sizeParam, "size query parameter must be present");
        assertEquals("query", sizeParam.path("in").asText());
        assertEquals("integer", sizeParam.path("schema").path("type").asText());
        assertEquals("10", sizeParam.path("schema").path("default").asText());
        assertEquals(1, sizeParam.path("schema").path("minimum").asInt());
        assertEquals(100, sizeParam.path("schema").path("maximum").asInt());

        // Verify 'sort' parameter
        assertNotNull(sortParam, "sort query parameter must be present");
        assertEquals("query", sortParam.path("in").asText());
        assertEquals("array", sortParam.path("schema").path("type").asText());
        assertTrue(sortParam.path("description").asText().contains("Allowed: "),
                "Sort parameter must document allowed sort properties");
        assertFalse(sortParam.path("description").asText().contains("Allowed: ["),
                "Sort parameter must not have square brackets around allowed list");
        assertTrue(sortParam.path("description").asText().contains("Format: `property,direction`"),
                "Sort parameter must document format");
        assertEquals("Zero-based page index.", pageParam.path("description").asText());
        assertTrue(sizeParam.path("description").asText().contains("Records per page."));

        // Verify GET /api/v1/dashboard/low-stock has clean query parameters and no request body
        JsonNode lowStockGet = paths.path("/api/v1/dashboard/low-stock").path("get");
        assertFalse(lowStockGet.isMissingNode(), "GET /api/v1/dashboard/low-stock must be documented");
        assertTrue(lowStockGet.path("requestBody").isMissingNode(), "GET /api/v1/dashboard/low-stock must not have a request body");
    }

    @Test
    @DisplayName("Swagger OpenAPI documents clean parameters for GET /api/v1/users")
    void testUsersPaginationDocumentation() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode usersGet = root.path("paths").path("/api/v1/users").path("get");
        assertFalse(usersGet.isMissingNode(), "GET /api/v1/users must be documented");

        JsonNode parameters = usersGet.path("parameters");
        assertTrue(parameters.isArray(), "Parameters must be an array");

        System.out.println("USERS_OPENAPI_PARAMETERS_JSON_START");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters));
        System.out.println("USERS_OPENAPI_PARAMETERS_JSON_END");

        // Ensure page, size, and sort exist
        boolean hasPage = false;
        boolean hasSize = false;
        boolean hasSort = false;
        for (JsonNode param : parameters) {
            String name = param.path("name").asText();
            if ("page".equals(name)) hasPage = true;
            if ("size".equals(name)) hasSize = true;
            if ("sort".equals(name)) hasSort = true;
        }
        assertTrue(hasPage, "page parameter must be present");
        assertTrue(hasSize, "size parameter must be present");
        assertTrue(hasSort, "sort parameter must be present");
    }
}
