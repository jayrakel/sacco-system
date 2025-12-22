-- Fix deposit_products.created_by to allow NULL for admin users
ALTER TABLE deposit_products ALTER COLUMN created_by DROP NOT NULL;
