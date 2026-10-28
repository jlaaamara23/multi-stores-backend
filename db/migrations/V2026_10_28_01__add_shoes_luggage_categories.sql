-- Add SHOES and LUGGAGE store categories (PostgreSQL).
--
-- Why: Hibernate created a CHECK constraint on stores.category listing the
-- allowed enum values. ddl-auto=update never changes existing constraints,
-- so new enum values are rejected by the database until this runs.
--
-- Run ONCE, manually, after taking a backup, BEFORE deploying the backend
-- that contains SHOES/LUGGAGE. Safe to re-run. Existing rows are unaffected.

BEGIN;

ALTER TABLE stores DROP CONSTRAINT IF EXISTS stores_category_check;

ALTER TABLE stores ADD CONSTRAINT stores_category_check CHECK (
  category IN (
    'KITCHEN', 'SPICES', 'GROCERY', 'CLOTHING', 'WOMEN_UNDERWEAR',
    'KIDS_UNDERWEAR', 'CHILD_GAME', 'ELECTRONICS', 'BEAUTY', 'HOME_DECOR',
    'SPORTS', 'PETS', 'HEALTH', 'TOOLS', 'OFFICE',
    'SHOES', 'LUGGAGE'
  )
);

COMMIT;

-- Rollback (only if no store uses SHOES/LUGGAGE yet):
-- BEGIN;
-- ALTER TABLE stores DROP CONSTRAINT IF EXISTS stores_category_check;
-- ALTER TABLE stores ADD CONSTRAINT stores_category_check CHECK (category IN (
--   'KITCHEN','SPICES','GROCERY','CLOTHING','WOMEN_UNDERWEAR','KIDS_UNDERWEAR','CHILD_GAME',
--   'ELECTRONICS','BEAUTY','HOME_DECOR','SPORTS','PETS','HEALTH','TOOLS','OFFICE'));
-- COMMIT;
