package com.example.multi_stores.repository;

import com.example.multi_stores.entity.ProductSizeStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSizeStockRepository extends JpaRepository<ProductSizeStock, Long> {

    List<ProductSizeStock> findByProductIdOrderBySize(Long productId);

    Optional<ProductSizeStock> findByProductIdAndSize(Long productId, String size);

    void deleteByProductId(Long productId);
}
