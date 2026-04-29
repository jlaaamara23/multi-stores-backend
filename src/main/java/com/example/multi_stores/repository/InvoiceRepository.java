package com.example.multi_stores.repository;

import com.example.multi_stores.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByOrderId(Long orderId);
}
