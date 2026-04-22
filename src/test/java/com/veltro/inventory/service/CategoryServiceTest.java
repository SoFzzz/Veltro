package com.veltro.inventory.service;

import com.veltro.inventory.dto.catalog.CategoryResponse;
import com.veltro.inventory.dto.catalog.UpdateCategoryRequest;
import com.veltro.inventory.exception.DuplicateResourceException;
import com.veltro.inventory.exception.InactiveResourceExistsException;
import com.veltro.inventory.mapper.CategoryMapper;
import com.veltro.inventory.model.CategoryEntity;
import com.veltro.inventory.repository.CategoryRepository;
import com.veltro.inventory.security.VeltroUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long BUSINESS_ID = 100L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        authenticateAsTenantUser();
        categoryService = new CategoryService(categoryRepository, categoryMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("update allows keeping the same name on the current category")
    void update_sameNameOnCurrentCategory_succeeds() {
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Bebidas",
                "Categoria actualizada",
                null
        );

        CategoryEntity existing = new CategoryEntity();
        existing.setId(1L);
        existing.setActive(true);
        existing.setName("Bebidas");

        CategoryResponse response = new CategoryResponse(
                1L,
                "Bebidas",
                "Categoria actualizada",
                null,
                true,
                List.of()
        );

        when(categoryRepository.findByNameAndBusinessId("Bebidas", BUSINESS_ID))
                .thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndActiveTrueAndBusinessId(1L, BUSINESS_ID))
                .thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(existing);
        when(categoryMapper.toResponse(existing)).thenReturn(response);

        CategoryResponse result = categoryService.update(1L, request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("update throws DuplicateResourceException when name belongs to another active category")
    void update_nameCollisionWithActiveCategory_throwsDuplicateResourceException() {
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Bebidas",
                "Categoria actualizada",
                null
        );

        CategoryEntity otherActive = new CategoryEntity();
        otherActive.setId(2L);
        otherActive.setActive(true);
        otherActive.setName("Bebidas");

        when(categoryRepository.findByNameAndBusinessId("Bebidas", BUSINESS_ID))
                .thenReturn(Optional.of(otherActive));

        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("name")
                .hasMessageContaining("Bebidas");
    }

    @Test
    @DisplayName("update throws InactiveResourceExistsException when name belongs to another inactive category")
    void update_nameCollisionWithInactiveCategory_throwsInactiveResourceExistsException() {
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Bebidas",
                "Categoria actualizada",
                null
        );

        CategoryEntity otherInactive = new CategoryEntity();
        otherInactive.setId(2L);
        otherInactive.setActive(false);
        otherInactive.setName("Bebidas");

        when(categoryRepository.findByNameAndBusinessId("Bebidas", BUSINESS_ID))
                .thenReturn(Optional.of(otherInactive));

        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(InactiveResourceExistsException.class)
                .hasMessageContaining("Consider reactivating it")
                .hasMessageContaining("id=2");
    }

    private void authenticateAsTenantUser() {
        VeltroUserDetails principal = new VeltroUserDetails(
                "category-tester",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                USER_ID,
                BUSINESS_ID
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

