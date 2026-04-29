-- Add quantity per color variant and color on order items.
-- Run once against your multi_stores database.

USE multi_stores;

-- Add quantity to product_color_variant (default 0 for existing rows)
ALTER TABLE product_color_variant ADD COLUMN quantity INT NOT NULL DEFAULT 0;

-- Add color to order_items (for orders that include a color variant)
ALTER TABLE order_items ADD COLUMN color VARCHAR(50) NULL;
