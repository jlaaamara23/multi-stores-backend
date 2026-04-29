-- Fix "Data truncated for column 'category'" when using CHILD_GAME.
-- Run this once against your multi_stores database (e.g. in MySQL Workbench).

USE multi_stores;

-- Widen the category column so it can store 'CHILD_GAME' (and future enum values).
ALTER TABLE stores MODIFY COLUMN category VARCHAR(20) NOT NULL;
