-- Draft products (PostgreSQL).
--
-- Adds products.status (DRAFT / PUBLISHED) and allows products.price to be
-- NULL, which the application allows only for DRAFT products.
--
-- Run ONCE, manually, after taking a backup, BEFORE deploying the backend
-- that contains ProductStatus. Safe to re-run. All existing products become
-- PUBLISHED, so the live shop looks exactly the same afterwards.

BEGIN;

ALTER TABLE products ADD COLUMN IF NOT EXISTS status VARCHAR(20);
UPDATE products SET status = 'PUBLISHED' WHERE status IS NULL;
ALTER TABLE products ALTER COLUMN status SET DEFAULT 'PUBLISHED';

ALTER TABLE products DROP CONSTRAINT IF EXISTS products_status_check;
ALTER TABLE products ADD CONSTRAINT products_status_check
  CHECK (status IS NULL OR status IN ('DRAFT', 'PUBLISHED'));

ALTER TABLE products ALTER COLUMN price DROP NOT NULL;

-- Defence in depth: a published product must have a price.
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_published_has_price;
ALTER TABLE products ADD CONSTRAINT products_published_has_price
  CHECK (status = 'DRAFT' OR price IS NOT NULL);

COMMIT;

-- Rollback (only after deleting or pricing all drafts):
-- BEGIN;
-- ALTER TABLE products DROP CONSTRAINT IF EXISTS products_published_has_price;
-- ALTER TABLE products ALTER COLUMN price SET NOT NULL;
-- ALTER TABLE products DROP CONSTRAINT IF EXISTS products_status_check;
-- ALTER TABLE products DROP COLUMN IF EXISTS status;
-- COMMIT;
