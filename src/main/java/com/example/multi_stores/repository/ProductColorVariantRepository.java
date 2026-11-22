package com.example.multi_stores.repository;

import com.example.multi_stores.entity.ProductColorVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductColorVariantRepository extends JpaRepository<ProductColorVariant, Long> {

    List<ProductColorVariant> findByProductIdOrderByColor(Long productId);

    List<ProductColorVariant> findByProductIdInOrderByColor(Collection<Long> productIds);

    Optional<ProductColorVariant> findByProductIdAndColor(Long productId, String color);

    void deleteByProductId(Long productId);
}
