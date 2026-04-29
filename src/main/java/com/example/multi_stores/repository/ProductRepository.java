package com.example.multi_stores.repository;

import com.example.multi_stores.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStoreId(Long storeId);

    List<Product> findByStoreIdAndCategoryId(Long storeId, Long categoryId);

    List<Product> findByIdIn(Collection<Long> ids);
}
