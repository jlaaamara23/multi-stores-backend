package com.example.multi_stores.repository;

import com.example.multi_stores.entity.Store;
import com.example.multi_stores.entity.StoreCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByName(String name);

    Optional<Store> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    List<Store> findByCategory(StoreCategory category);
}
