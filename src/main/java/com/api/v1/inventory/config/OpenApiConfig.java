package com.api.v1.inventory.config;

import com.api.v1.inventory.controller.CategoryController;
import com.api.v1.inventory.controller.CustomerController;
import com.api.v1.inventory.controller.DashboardController;
import com.api.v1.inventory.controller.ProductController;
import com.api.v1.inventory.controller.ReportController;
import com.api.v1.inventory.controller.StockController;
import com.api.v1.inventory.controller.SupplierController;
import com.api.v1.inventory.controller.UserController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.method.HandlerMethod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Management System API")
                        .version("1.0.0")
                        .contact(new Contact().name("SETEC Year 4 Team")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    // unshow lock icon on specific endpoint
    @Bean
    public GlobalOpenApiCustomizer hideLockOnAuthEndpoints() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            // Change "/api/auth" to match your actual auth URL prefix (e.g., "/auth" or "/api/v1/auth")
            if (path.startsWith("/api/v1/auth/login") ||
                    path.startsWith("/api/v1/auth/forgot-password") ||
                    path.startsWith("/api/v1/auth/reset-password")
                ) {
                pathItem.readOperations().forEach(operation ->
                        operation.setSecurity(Collections.emptyList())
                );
            }
        });
    }

    @Bean
    public OperationCustomizer paginationOperationCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            MethodParameter pageableParam = null;
            for (MethodParameter param : handlerMethod.getMethodParameters()) {
                if (Pageable.class.isAssignableFrom(param.getParameterType())) {
                    pageableParam = param;
                    break;
                }
            }

            if (pageableParam != null) {
                PageableDefault pageableDefault = pageableParam.getParameterAnnotation(PageableDefault.class);
                int defaultSize = (pageableDefault != null) ? pageableDefault.size() : 10;
                String defaultSortProp = (pageableDefault != null && pageableDefault.sort().length > 0)
                        ? pageableDefault.sort()[0]
                        : "id";
                String defaultDirection = (pageableDefault != null && pageableDefault.direction() != null)
                        ? pageableDefault.direction().name().toLowerCase()
                        : "asc";
                String defaultSort = defaultSortProp + "," + defaultDirection;

                List<String> allowedProperties = getAllowedSortProperties(handlerMethod);

                // Clean up any existing page/size/sort parameters generated automatically
                if (operation.getParameters() != null) {
                    operation.getParameters().removeIf(p ->
                            "page".equalsIgnoreCase(p.getName()) ||
                            "size".equalsIgnoreCase(p.getName()) ||
                            "sort".equalsIgnoreCase(p.getName())
                    );
                } else {
                    operation.setParameters(new ArrayList<>());
                }

                Parameter pageParam = new QueryParameter()
                        .name("page")
                        .required(false)
                        .description("Zero-based page index.")
                        .schema(new IntegerSchema()._default(0).minimum(BigDecimal.ZERO));

                Parameter sizeParam = new QueryParameter()
                        .name("size")
                        .required(false)
                        .description("Records per page. Requests requesting more than 100 records are capped at 100.")
                        .schema(new IntegerSchema()._default(defaultSize).minimum(BigDecimal.ONE).maximum(new BigDecimal("100")));

                String sortDescription = (allowedProperties != null && !allowedProperties.isEmpty())
                        ? "Format: `property,direction` (asc or desc). Repeat for multiple sorts. Allowed: " + String.join(", ", allowedProperties) + "."
                        : "Format: `property,direction` (asc or desc). Repeat for multiple sorts.";

                Parameter sortParam = new QueryParameter()
                        .name("sort")
                        .required(false)
                        .description(sortDescription)
                        .schema(new ArraySchema().items(new StringSchema()));

                if (allowedProperties != null && !allowedProperties.isEmpty()) {
                    String first = allowedProperties.get(0);
                    String second = allowedProperties.get(allowedProperties.size() - 1);
                    sortParam.addExample("single", new Example().summary("Single property sort").value(first + ",asc"));
                    sortParam.addExample("multi", new Example().summary("Multi-property sort").value(List.of(first + ",asc", second + ",desc")));
                }

                operation.addParametersItem(pageParam);
                operation.addParametersItem(sizeParam);
                operation.addParametersItem(sortParam);
            }

            return operation;
        };
    }

    private List<String> getAllowedSortProperties(HandlerMethod handlerMethod) {
        Class<?> beanType = handlerMethod.getBeanType();
        String methodName = handlerMethod.getMethod().getName();

        if (ReportController.class.equals(beanType)) {
            if ("getStockTransactionReport".equals(methodName)) {
                return ReportController.ALLOWED_STOCK_TRANSACTIONS_SORT_PROPERTIES;
            } else {
                return ReportController.ALLOWED_INVENTORY_REPORT_SORT_PROPERTIES;
            }
        } else if (ProductController.class.equals(beanType)) {
            return ProductController.ALLOWED_SORT_PROPERTIES;
        } else if (CategoryController.class.equals(beanType)) {
            return CategoryController.ALLOWED_SORT_PROPERTIES;
        } else if (CustomerController.class.equals(beanType)) {
            return CustomerController.ALLOWED_SORT_PROPERTIES;
        } else if (SupplierController.class.equals(beanType)) {
            return SupplierController.ALLOWED_SORT_PROPERTIES;
        } else if (UserController.class.equals(beanType)) {
            return UserController.ALLOWED_SORT_PROPERTIES;
        } else if (StockController.class.equals(beanType)) {
            return StockController.ALLOWED_SORT_PROPERTIES;
        } else if (DashboardController.class.equals(beanType)) {
            return DashboardController.ALLOWED_SORT_PROPERTIES;
        }

        log.warn("No ALLOWED_SORT_PROPERTIES constant found for controller: {}", beanType.getSimpleName());
        return Collections.emptyList();
    }
}
