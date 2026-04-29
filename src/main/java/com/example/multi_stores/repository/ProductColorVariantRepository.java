package com.example.multi_stores.repository;

import com.example.multi_stores.entity.ProductColorVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductColorVariantRepository extends JpaRepository<ProductColorVariant, Long> {

    List<ProductColorVariant> findByProductIdOrderByColor(Long productId);

    Optional<ProductColorVariant> findByProductIdAndColor(Long productId, String color);

    void deleteByProductId(Long productId);
}
