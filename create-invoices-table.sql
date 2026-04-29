-- Invoices table (created automatically by Hibernate if ddl-auto=update).
-- Run manually only if not using Hibernate ddl-auto.

USE multi_stores;

CREATE TABLE IF NOT EXISTS invoices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL UNIQUE,
  customer_email VARCHAR(255) NOT NULL,
  invoice_number VARCHAR(50) NOT NULL,
  html_content TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_invoice_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
