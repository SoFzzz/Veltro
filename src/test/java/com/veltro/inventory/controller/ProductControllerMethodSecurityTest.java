package com.veltro.inventory.controller;

import com.veltro.inventory.dto.catalog.ProductResponse;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductControllerMethodSecurityTest {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_CASHIER = "CASHIER";
    private static final int DEFAULT_PAGE_SIZE = 20;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("listInactiveProducts allows ADMIN role")
    void listInactiveProducts_allowsAdminRole() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(MethodSecurityTestConfig.class)) {
            ProductController controller = context.getBean(ProductController.class);
            ProductService productService = context.getBean(ProductService.class);

            when(productService.findAllInactive(any())).thenReturn(emptyPage());
            authenticate(ROLE_ADMIN);

            ResponseEntity<PageResponse<ProductResponse>> response =
                    controller.listInactiveProducts(PageRequest.of(0, DEFAULT_PAGE_SIZE));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(productService).findAllInactive(any());
        }
    }

    @Test
    @DisplayName("listInactiveProducts denies CASHIER role")
    void listInactiveProducts_deniesCashierRole() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(MethodSecurityTestConfig.class)) {
            ProductController controller = context.getBean(ProductController.class);
            ProductService productService = context.getBean(ProductService.class);

            authenticate(ROLE_CASHIER);

            assertThatThrownBy(() -> controller.listInactiveProducts(PageRequest.of(0, DEFAULT_PAGE_SIZE)))
                    .isInstanceOf(AccessDeniedException.class);

            verify(productService, never()).findAllInactive(any());
        }
    }

    private void authenticate(String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private PageResponse<ProductResponse> emptyPage() {
        return new PageResponse<>(List.of(), 0, DEFAULT_PAGE_SIZE, 0L, 0, true);
    }

    @Configuration
    @EnableMethodSecurity
    @EnableAspectJAutoProxy
    static class MethodSecurityTestConfig {

        @Bean
        ProductService productService() {
            return mock(ProductService.class);
        }

        @Bean
        ProductController productController(ProductService productService) {
            return new ProductController(productService);
        }
    }
}
