package com.example.multi_stores.service;

import com.example.multi_stores.dto.ColorVariantDto;
import com.example.multi_stores.dto.ProductRequest;
import com.example.multi_stores.dto.ProductResponse;
import com.example.multi_stores.dto.SizeStockDto;
import com.example.multi_stores.entity.Product;
import com.example.multi_stores.entity.ProductColorVariant;
import com.example.multi_stores.entity.ProductSizeStock;
import com.example.multi_stores.entity.Store;
import com.example.multi_stores.entity.UserRole;
import com.example.multi_stores.repository.ProductColorVariantRepository;
import com.example.multi_stores.repository.OrderItemRepository;
import com.example.multi_stores.repository.ProductRepository;
import com.example.multi_stores.repository.ProductSizeStockRepository;
import com.example.multi_stores.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSizeStockRepository productSizeStockRepository;
    private final ProductColorVariantRepository productColorVariantRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreRepository storeRepository;

    public ProductService(ProductRepository productRepository,
                         ProductSizeStockRepository productSizeStockRepository,
                         ProductColorVariantRepository productColorVariantRepository,
                         OrderItemRepository orderItemRepository,
                         StoreRepository storeRepository) {
        this.productRepository = productRepository;
        this.productSizeStockRepository = productSizeStockRepository;
        this.productColorVariantRepository = productColorVariantRepository;
        this.orderItemRepository = orderItemRepository;
        this.storeRepository = storeRepository;
    }

    public List<ProductResponse> findByStoreId(Long storeId) {
        return productRepository.findByStoreId(storeId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> findByStoreIdAndCategory(Long storeId, Long categoryId) {
        if (categoryId == null) {
            return findByStoreId(storeId);
        }
        return productRepository.findByStoreIdAndCategoryId(storeId, categoryId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse addProduct(ProductRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + request.getStoreId()));
        String imageUrlsStr = null;
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            imageUrlsStr = request.getImageUrls().stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
            if (imageUrlsStr.isEmpty()) imageUrlsStr = null;
        }
        int totalStock = request.getStockQuantity() != null ? request.getStockQuantity() : 0;
        if (request.getSizeStocks() != null && !request.getSizeStocks().isEmpty()) {
            totalStock = request.getSizeStocks().stream()
                    .filter(s -> s.getQuantity() != null && s.getQuantity() > 0)
                    .mapToInt(SizeStockDto::getQuantity)
                    .sum();
        }
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(totalStock)
                .storeId(store.getId())
                .categoryId(request.getCategoryId())
                .imageUrls(imageUrlsStr)
                .build();
        product = productRepository.save(product);
        saveSizeStocks(product.getId(), request.getSizeStocks());
        saveColorVariants(product.getId(), request.getColorVariants());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateStock(Long productId, int delta) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        int newStock = product.getStockQuantity() + delta;
        if (newStock < 0) {
            throw new IllegalStateException("Insufficient stock for product: " + product.getName());
        }
        product.setStockQuantity(newStock);
        product = productRepository.save(product);
        return toResponse(product);
    }

    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request, Long userId, UserRole userRole) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        Store store = storeRepository.findById(product.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        if (userRole != UserRole.ADMIN) {
            throw new IllegalStateException("Only admin can update this product");
        }
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        int totalStock = request.getStockQuantity() != null ? request.getStockQuantity() : 0;
        if (request.getSizeStocks() != null && !request.getSizeStocks().isEmpty()) {
            totalStock = request.getSizeStocks().stream()
                    .filter(s -> s.getQuantity() != null && s.getQuantity() > 0)
                    .mapToInt(SizeStockDto::getQuantity)
                    .sum();
        }
        product.setStockQuantity(totalStock);
        product.setCategoryId(request.getCategoryId());
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            String imageUrlsStr = request.getImageUrls().stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
            product.setImageUrls(imageUrlsStr.isEmpty() ? null : imageUrlsStr);
        } else {
            product.setImageUrls(null);
        }
        product = productRepository.save(product);
        saveSizeStocks(product.getId(), request.getSizeStocks());
        saveColorVariants(product.getId(), request.getColorVariants());
        return toResponse(product);
    }

    @Transactional
    public void deleteProduct(Long productId, Long userId, UserRole userRole) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        Store store = storeRepository.findById(product.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        if (userRole != UserRole.ADMIN) {
            throw new IllegalStateException("Only admin can delete this product");
        }
        if (orderItemRepository.existsByProductId(product.getId())) {
            throw new IllegalStateException("Cannot delete this product because it is used in previous orders. Remove its image or set stock to 0 instead.");
        }
        productSizeStockRepository.deleteByProductId(product.getId());
        productColorVariantRepository.deleteByProductId(product.getId());
        productRepository.delete(product);
        productRepository.flush();
    }

    private void saveColorVariants(Long productId, List<ColorVariantDto> colorVariants) {
        if (productId == null) return;
        productColorVariantRepository.deleteByProductId(productId);
        if (colorVariants == null || colorVariants.isEmpty()) return;
        for (ColorVariantDto dto : colorVariants) {
            if (dto.getColor() == null || dto.getColor().isBlank()) continue;
            List<String> urls = dto.getImageUrls() != null ? dto.getImageUrls() : Collections.emptyList();
            String imageUrlsStr = urls.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining(","));
            if (imageUrlsStr.isEmpty()) imageUrlsStr = null;
            ProductColorVariant v = new ProductColorVariant();
            v.setProductId(productId);
            v.setColor(dto.getColor().trim());
            v.setImageUrls(imageUrlsStr);
            v.setQuantity(dto.getQuantity() != null && dto.getQuantity() > 0 ? dto.getQuantity() : 0);
            productColorVariantRepository.save(v);
        }
    }

    private List<ColorVariantDto> loadColorVariants(Long productId) {
        if (productId == null) return Collections.emptyList();
        return productColorVariantRepository.findByProductIdOrderByColor(productId).stream()
                .map(v -> new ColorVariantDto(v.getColor(), parseImageUrls(v.getImageUrls()), v.getQuantity() != null ? v.getQuantity() : 0))
                .collect(Collectors.toList());
    }

    private void saveSizeStocks(Long productId, List<SizeStockDto> sizeStocks) {
        if (productId == null) return;
        productSizeStockRepository.deleteByProductId(productId);
        productSizeStockRepository.flush();
        if (sizeStocks == null || sizeStocks.isEmpty()) return;
        // One row per size in DB (unique product_id + size). Merge duplicate payload rows by summing qty.
        Map<String, Integer> quantityBySize = new LinkedHashMap<>();
        for (SizeStockDto dto : sizeStocks) {
            if (dto.getSize() == null || dto.getSize().isBlank()) continue;
            String sizeKey = dto.getSize().trim().toUpperCase();
            int qty = dto.getQuantity() != null && dto.getQuantity() > 0 ? dto.getQuantity() : 0;
            quantityBySize.merge(sizeKey, qty, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : quantityBySize.entrySet()) {
            productSizeStockRepository.save(ProductSizeStock.builder()
                    .productId(productId)
                    .size(e.getKey())
                    .quantity(e.getValue())
                    .build());
        }
    }

    private List<SizeStockDto> loadSizeStocks(Long productId) {
        if (productId == null) return Collections.emptyList();
        return productSizeStockRepository.findByProductIdOrderBySize(productId).stream()
                .map(s -> new SizeStockDto(s.getSize(), s.getQuantity()))
                .collect(Collectors.toList());
    }

    private ProductResponse toResponse(Product product) {
        List<String> imageUrls = parseImageUrls(product.getImageUrls());
        List<SizeStockDto> sizeStock = loadSizeStocks(product.getId());
        List<ColorVariantDto> colorVariants = loadColorVariants(product.getId());
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .storeId(product.getStoreId())
                .categoryId(product.getCategoryId())
                .imageUrls(imageUrls)
                .sizeStock(sizeStock)
                .colorVariants(colorVariants)
                .build();
    }

    /** Returns available quantity for the product; if size is set, uses size stock; if color is set, uses color variant quantity. */
    public int getAvailableQuantity(Long productId, String size, String color) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (color != null && !color.isBlank()) {
            return productColorVariantRepository.findByProductIdAndColor(productId, color.trim())
                    .map(v -> v.getQuantity() != null ? v.getQuantity() : 0)
                    .orElse(0);
        }
        if (size != null && !size.isBlank()) {
            return productSizeStockRepository.findByProductIdAndSize(productId, size.trim().toUpperCase())
                    .map(ProductSizeStock::getQuantity)
                    .orElse(0);
        }
        return product.getStockQuantity() != null ? product.getStockQuantity() : 0;
    }

    /** Decrements stock for product; if size is set, decrements that size; if color is set, decrements that color variant. */
    @Transactional
    public void decrementStock(Long productId, String size, String color, int quantity) {
        if (color != null && !color.isBlank()) {
            ProductColorVariant cv = productColorVariantRepository.findByProductIdAndColor(productId, color.trim())
                    .orElseThrow(() -> new IllegalStateException("Color not found for product"));
            if (cv.getQuantity() == null || cv.getQuantity() < quantity) {
                throw new IllegalStateException("Insufficient stock for color " + color);
            }
            cv.setQuantity(cv.getQuantity() - quantity);
            productColorVariantRepository.save(cv);
            return;
        }
        if (size != null && !size.isBlank()) {
            ProductSizeStock ss = productSizeStockRepository.findByProductIdAndSize(productId, size.trim().toUpperCase())
                    .orElseThrow(() -> new IllegalStateException("Size not found for product"));
            if (ss.getQuantity() < quantity) {
                throw new IllegalStateException("Insufficient stock for size " + size);
            }
            ss.setQuantity(ss.getQuantity() - quantity);
            productSizeStockRepository.save(ss);
            Product product = productRepository.findById(productId).orElseThrow();
            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);
        } else {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
            if (product.getStockQuantity() < quantity) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }
            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);
        }
    }

    private static List<String> parseImageUrls(String imageUrlsStr) {
        if (imageUrlsStr == null || imageUrlsStr.isBlank()) return Collections.emptyList();
        return Arrays.stream(imageUrlsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
