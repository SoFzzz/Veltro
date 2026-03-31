package com.veltro.inventory.service;

import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductMatchingService")
class ProductMatchingServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductMatchingService service;

    @Test
    @DisplayName("findMatch returns the only candidate for the tenant")
    void findMatch_returnsOnlyCandidate() {
        ProductEntity sprite = product(1L, "Sprite 500 ml", "750123");
        when(productRepository.findTop10ByActiveTrueAndBusinessIdAndNameContainingIgnoreCase(3L, "sprite"))
                .thenReturn(List.of(sprite));

        Optional<ProductEntity> match = service.findMatch("Sprite Sabor Lima Limon", 3L);

        assertThat(match).contains(sprite);
        verify(productRepository).findTop10ByActiveTrueAndBusinessIdAndNameContainingIgnoreCase(3L, "sprite");
    }

    @Test
    @DisplayName("findMatch returns empty when multiple variants remain ambiguous")
    void findMatch_returnsEmptyForAmbiguousVariants() {
        ProductEntity small = product(1L, "Sprite 500 ml", "750123");
        ProductEntity large = product(2L, "Sprite 1.5 L", "750999");
        when(productRepository.findTop10ByActiveTrueAndBusinessIdAndNameContainingIgnoreCase(3L, "sprite"))
                .thenReturn(List.of(small, large));

        Optional<ProductEntity> match = service.findMatch("Sprite", 3L);

        assertThat(match).isEmpty();
    }

    @Test
    @DisplayName("findMatch uses volume to disambiguate variants")
    void findMatch_usesVolumeToDisambiguateVariants() {
        ProductEntity small = product(1L, "Sprite 500 ml", "750123");
        ProductEntity large = product(2L, "Sprite 1.5 L", "750999");
        when(productRepository.findTop10ByActiveTrueAndBusinessIdAndNameContainingIgnoreCase(3L, "sprite"))
                .thenReturn(List.of(small, large));

        Optional<ProductEntity> match = service.findMatch("Sprite 500 ml", 3L);

        assertThat(match).contains(small);
    }

    @Test
    @DisplayName("findMatch returns empty when there are no candidates")
    void findMatch_returnsEmptyWhenNoCandidates() {
        when(productRepository.findTop10ByActiveTrueAndBusinessIdAndNameContainingIgnoreCase(3L, "sprite"))
                .thenReturn(List.of());

        Optional<ProductEntity> match = service.findMatch("Sprite Sabor Lima Limon", 3L);

        assertThat(match).isEmpty();
    }

    private ProductEntity product(Long id, String name, String barcode) {
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setBarcode(barcode);
        entity.setActive(true);
        entity.setBusinessId(3L);
        return entity;
    }
}
