package com.example.multi_stores.service;

import com.example.multi_stores.dto.CloudinaryImportRequest;
import com.example.multi_stores.entity.Product;
import com.example.multi_stores.entity.ProductColorVariant;
import com.example.multi_stores.entity.ProductStatus;
import com.example.multi_stores.entity.Store;
import com.example.multi_stores.repository.OrderItemRepository;
import com.example.multi_stores.repository.ProductColorVariantRepository;
import com.example.multi_stores.repository.ProductRepository;
import com.example.multi_stores.repository.ProductSizeStockRepository;
import com.example.multi_stores.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The admin Cloudinary folder import must never publish products or touch products that already
 * use an image (e.g. the grouped products created by the Upload Kit import).
 */
class CloudinaryImportSafetyTest {

    private static final String USED_BY_PRODUCT = "https://res.cloudinary.com/x/image/upload/multi-stores/a.jpg";
    private static final String USED_BY_VARIANT = "https://res.cloudinary.com/x/image/upload/multi-stores/b.jpg";
    private static final String NEW_IMAGE = "https://res.cloudinary.com/x/image/upload/multi-stores/c.jpg";

    private ProductRepository productRepository;
    private ProductColorVariantRepository variantRepository;
    private StoreRepository storeRepository;
    private FileStorageService fileStorageService;
    private ProductService service;
    private final List<Product> saved = new ArrayList<>();
    private Product existing;

    @BeforeEach
    void setUp() throws Exception {
        productRepository = mock(ProductRepository.class);
        variantRepository = mock(ProductColorVariantRepository.class);
        storeRepository = mock(StoreRepository.class);
        fileStorageService = mock(FileStorageService.class);
        service = new ProductService(productRepository, mock(ProductSizeStockRepository.class), variantRepository,
                mock(OrderItemRepository.class), storeRepository, fileStorageService);

        Store importStore = new Store();
        importStore.setId(10L);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(importStore));

        // An existing draft product (in ANOTHER store) and a colour variant already use two of the images.
        existing = Product.builder().name("Grouped kaftan").storeId(99L).imageUrls(USED_BY_PRODUCT)
                .status(ProductStatus.DRAFT).build();
        existing.setId(1L);
        ProductColorVariant variant = new ProductColorVariant();
        variant.setProductId(1L);
        variant.setColor("Black");
        variant.setImageUrls(USED_BY_VARIANT);
        when(productRepository.findAll()).thenReturn(List.of(existing));
        when(variantRepository.findAll()).thenReturn(List.of(variant));

        when(fileStorageService.listImagesInFolder(anyString(), anyInt())).thenReturn(List.of(
                new FileStorageService.CloudinaryAsset(USED_BY_PRODUCT, "multi-stores/a"),
                new FileStorageService.CloudinaryAsset(USED_BY_VARIANT, "multi-stores/b"),
                new FileStorageService.CloudinaryAsset(NEW_IMAGE, "multi-stores/c")));

        AtomicLong ids = new AtomicLong(100);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getId() == null) p.setId(ids.incrementAndGet());
            saved.add(p);
            return p;
        });
    }

    private CloudinaryImportRequest request() {
        CloudinaryImportRequest r = new CloudinaryImportRequest();
        r.setStoreId(10L);
        r.setDefaultPrice(new BigDecimal("50"));
        r.setDefaultStock(10);
        return r;
    }

    @Test
    void skipsImagesUsedByAnyProductOrColourVariantInAnyStore() throws Exception {
        Map<String, Object> result = service.importFromCloudinary(request());
        assertEquals(1, result.get("imported"));
        assertEquals(2, result.get("skipped"));
        assertEquals(1, saved.size());
        assertEquals(NEW_IMAGE, saved.get(0).getImageUrls());
    }

    @Test
    void importedProductsAreDraftsNeverPublished() throws Exception {
        service.importFromCloudinary(request());
        assertEquals(ProductStatus.DRAFT, saved.get(0).getStatus());
    }

    @Test
    void existingProductsAreNotModified() throws Exception {
        service.importFromCloudinary(request());
        assertEquals(ProductStatus.DRAFT, existing.getStatus());
        assertNull(existing.getPrice());
        assertEquals(false, saved.contains(existing));
    }

    @Test
    void worksWithoutDefaultPriceBecauseProductsAreDrafts() throws Exception {
        CloudinaryImportRequest r = request();
        r.setDefaultPrice(null);
        Map<String, Object> result = service.importFromCloudinary(r);
        assertEquals(1, result.get("imported"));
        assertNull(saved.get(0).getPrice());
    }
}
