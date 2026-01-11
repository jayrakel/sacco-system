-- Migration: Create Accounting Tables (Chart of Accounts & Journal Entries)
-- Version: V1.2
-- Description: Implements double-entry accounting system

-- ============================================================================
-- CHART OF ACCOUNTS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS chart_of_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_code VARCHAR(20) NOT NULL UNIQUE,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'INCOME', 'EXPENSE')),
    category VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_account BOOLEAN NOT NULL DEFAULT FALSE,
    parent_account_id UUID REFERENCES chart_of_accounts(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Indexes for Chart of Accounts
CREATE INDEX idx_coa_account_code ON chart_of_accounts(account_code);
CREATE INDEX idx_coa_account_type ON chart_of_accounts(account_type);
CREATE INDEX idx_coa_active ON chart_of_accounts(active);

-- ============================================================================
-- JOURNAL ENTRIES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES chart_of_accounts(id),
    transaction_id UUID REFERENCES transactions(id),
    entry_type VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount DECIMAL(15, 2) NOT NULL CHECK (amount >= 0),
    transaction_date DATE NOT NULL,
    description VARCHAR(500) NOT NULL,
    reference_number VARCHAR(255) NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_id UUID,
    posted BOOLEAN NOT NULL DEFAULT TRUE,
    reversed BOOLEAN NOT NULL DEFAULT FALSE,
    reversal_entry_id UUID REFERENCES journal_entries(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

-- Indexes for Journal Entries
CREATE INDEX idx_je_account_id ON journal_entries(account_id);
CREATE INDEX idx_je_transaction_id ON journal_entries(transaction_id);
CREATE INDEX idx_je_reference_number ON journal_entries(reference_number);
CREATE INDEX idx_je_reference_type_id ON journal_entries(reference_type, reference_id);
CREATE INDEX idx_je_transaction_date ON journal_entries(transaction_date);
CREATE INDEX idx_je_posted ON journal_entries(posted);

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE chart_of_accounts IS 'Stores the chart of accounts (GL account master data)';
COMMENT ON TABLE journal_entries IS 'Stores all double-entry journal entries for General Ledger';

COMMENT ON COLUMN chart_of_accounts.account_code IS 'Unique account code (e.g., 1200 for Loans Receivable)';
COMMENT ON COLUMN chart_of_accounts.system_account IS 'If true, account cannot be deleted';
COMMENT ON COLUMN journal_entries.entry_type IS 'DEBIT or CREDIT';
COMMENT ON COLUMN journal_entries.reference_number IS 'Links DR and CR entries together';
COMMENT ON COLUMN journal_entries.posted IS 'If false, entry is in draft state';

