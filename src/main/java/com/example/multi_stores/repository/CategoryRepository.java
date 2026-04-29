package com.example.multi_stores.repository;

import com.example.multi_stores.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByStoreId(Long storeId);
}
