-- Migration to fix deposit_products.created_by to allow NULL for admin users
-- This allows ADMIN users without Member records to create deposit products

ALTER TABLE deposit_products ALTER COLUMN created_by DROP NOT NULL;
