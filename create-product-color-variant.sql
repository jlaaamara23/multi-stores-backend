-- Add table for product color variants (same product, different colors, each with its own images).
-- Run once against your multi_stores database.

USE multi_stores;

CREATE TABLE IF NOT EXISTS product_color_variant (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  color VARCHAR(50) NOT NULL,
  image_urls VARCHAR(2000),
  UNIQUE KEY uk_product_color (product_id, color),
  CONSTRAINT fk_color_variant_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
