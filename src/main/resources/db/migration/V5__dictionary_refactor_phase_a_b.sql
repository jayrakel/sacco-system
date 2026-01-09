-- Dictionary Refactor Phase A-B: Schema Updates
-- Date: 2026-01-07
-- Purpose: Add new fields from dictionary and migrate data from old fields

-- ========================================
-- BENEFICIARIES TABLE
-- ========================================
-- Add new columns as nullable first
ALTER TABLE beneficiaries ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);
ALTER TABLE beneficiaries ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);
ALTER TABLE beneficiaries ADD COLUMN IF NOT EXISTS identity_number VARCHAR(255);
ALTER TABLE beneficiaries ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE beneficiaries ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE beneficiaries ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE beneficiaries ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';
ALTER TABLE beneficiaries ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';

-- Migrate data from full_name to first_name/last_name
UPDATE beneficiaries
SET first_name = SPLIT_PART(full_name, ' ', 1),
    last_name = CASE
        WHEN full_name LIKE '% %' THEN SUBSTRING(full_name FROM POSITION(' ' IN full_name) + 1)
        ELSE full_name
    END
WHERE first_name IS NULL AND full_name IS NOT NULL;

-- Copy identity data
UPDATE beneficiaries
SET identity_number = id_number
WHERE identity_number IS NULL AND id_number IS NOT NULL;

-- Make columns NOT NULL after data migration
ALTER TABLE beneficiaries ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE beneficiaries ALTER COLUMN last_name SET NOT NULL;

-- ========================================
-- MEMBERS TABLE
-- ========================================
ALTER TABLE members ADD COLUMN IF NOT EXISTS national_id VARCHAR(255);
ALTER TABLE members ADD COLUMN IF NOT EXISTS membership_date TIMESTAMP;
ALTER TABLE members ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE members ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';
ALTER TABLE members ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';

-- Migrate data
UPDATE members
SET national_id = id_number
WHERE national_id IS NULL AND id_number IS NOT NULL;

UPDATE members
SET membership_date = registration_date
WHERE membership_date IS NULL AND registration_date IS NOT NULL;

-- Make NOT NULL
ALTER TABLE members ALTER COLUMN national_id SET NOT NULL;

-- Add unique constraint
ALTER TABLE members ADD CONSTRAINT uk_members_national_id UNIQUE (national_id);

-- ========================================
-- USERS TABLE
-- ========================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS user_status VARCHAR(255) DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';

-- Migrate data
UPDATE users
SET user_id = id,
    username = email,
    password_hash = password
WHERE user_id IS NULL;

-- Make NOT NULL
ALTER TABLE users ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;

-- Add unique constraints
ALTER TABLE users ADD CONSTRAINT uk_users_user_id UNIQUE (user_id);
ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);

-- ========================================
-- LOAN_PRODUCTS TABLE
-- ========================================
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS product_code VARCHAR(255);
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS currency_code VARCHAR(255) DEFAULT 'KES';
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';

-- Migrate data
UPDATE loan_products
SET product_code = CONCAT('LP', LPAD(CAST(id AS VARCHAR), 6, '0')),
    product_name = name,
    currency_code = 'KES'
WHERE product_code IS NULL;

-- Make NOT NULL
ALTER TABLE loan_products ALTER COLUMN product_code SET NOT NULL;
ALTER TABLE loan_products ALTER COLUMN product_name SET NOT NULL;
ALTER TABLE loan_products ALTER COLUMN currency_code SET NOT NULL;

-- Add unique constraint
ALTER TABLE loan_products ADD CONSTRAINT uk_loan_products_code UNIQUE (product_code);

-- ========================================
-- SAVINGS_PRODUCTS TABLE
-- ========================================
ALTER TABLE savings_products ADD COLUMN IF NOT EXISTS product_code VARCHAR(255);
ALTER TABLE savings_products ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
ALTER TABLE savings_products ADD COLUMN IF NOT EXISTS currency_code VARCHAR(255) DEFAULT 'KES';
ALTER TABLE savings_products ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE savings_products ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE savings_products ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE savings_products ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';
ALTER TABLE savings_products ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';

-- Migrate data
UPDATE savings_products
SET product_code = CONCAT('SP', LPAD(CAST(id AS VARCHAR), 6, '0')),
    product_name = name,
    currency_code = 'KES'
WHERE product_code IS NULL;

-- Make NOT NULL
ALTER TABLE savings_products ALTER COLUMN product_code SET NOT NULL;
ALTER TABLE savings_products ALTER COLUMN product_name SET NOT NULL;
ALTER TABLE savings_products ALTER COLUMN currency_code SET NOT NULL;

-- Add unique constraint
ALTER TABLE savings_products ADD CONSTRAINT uk_savings_products_code UNIQUE (product_code);

-- ========================================
-- SAVINGS_ACCOUNTS TABLE
-- ========================================
ALTER TABLE savings_accounts ADD COLUMN IF NOT EXISTS balance_amount NUMERIC(38,2) DEFAULT 0;
ALTER TABLE savings_accounts ADD COLUMN IF NOT EXISTS account_status VARCHAR(255) DEFAULT 'ACTIVE';
ALTER TABLE savings_accounts ADD COLUMN IF NOT EXISTS currency_code VARCHAR(255) DEFAULT 'KES';
ALTER TABLE savings_accounts ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE savings_accounts ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';
ALTER TABLE savings_accounts ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';

-- Migrate data
UPDATE savings_accounts
SET balance_amount = balance,
    account_status = COALESCE(status, 'ACTIVE'),
    currency_code = 'KES'
WHERE balance_amount IS NULL;

-- Make NOT NULL
ALTER TABLE savings_accounts ALTER COLUMN currency_code SET NOT NULL;

-- ========================================
-- LOANS TABLE
-- ========================================
ALTER TABLE loans ADD COLUMN IF NOT EXISTS loan_status VARCHAR(255);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS total_outstanding_amount NUMERIC(38,2) DEFAULT 0;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS approved_amount NUMERIC(38,2);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS disbursed_amount NUMERIC(38,2);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS outstanding_principal NUMERIC(38,2) DEFAULT 0;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS outstanding_interest NUMERIC(38,2) DEFAULT 0;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS maturity_date DATE;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS currency_code VARCHAR(255) DEFAULT 'KES';
ALTER TABLE loans ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';
ALTER TABLE loans ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';

-- Migrate data
UPDATE loans
SET loan_status = status,
    total_outstanding_amount = COALESCE(loan_balance, 0),
    approved_amount = principal_amount,
    disbursed_amount = principal_amount,
    outstanding_principal = COALESCE(loan_balance, 0),
    currency_code = 'KES'
WHERE loan_status IS NULL;

-- ========================================
-- EMPLOYMENT_DETAILS TABLE
-- ========================================
ALTER TABLE employment_details ADD COLUMN IF NOT EXISTS employment_terms VARCHAR(255);
ALTER TABLE employment_details ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE employment_details ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE employment_details ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE employment_details ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';
ALTER TABLE employment_details ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';

-- Migrate data
UPDATE employment_details
SET employment_terms = COALESCE(terms, 'PERMANENT')
WHERE employment_terms IS NULL;

-- ========================================
-- LOAN_GUARANTORS TABLE
-- ========================================
ALTER TABLE loan_guarantors ADD COLUMN IF NOT EXISTS guaranteed_amount NUMERIC(38,2) DEFAULT 0;
ALTER TABLE loan_guarantors ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE loan_guarantors ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE loan_guarantors ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE loan_guarantors ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';
ALTER TABLE loan_guarantors ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) DEFAULT 'SYSTEM_MIGRATION';

-- Migrate data
UPDATE loan_guarantors
SET guaranteed_amount = COALESCE(guarantee_amount, 0)
WHERE guaranteed_amount IS NULL;

-- Make NOT NULL
ALTER TABLE loan_guarantors ALTER COLUMN guaranteed_amount SET NOT NULL;

-- Migration complete

