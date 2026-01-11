-- Migration Script: Fix Existing Disbursed Loans
-- Purpose: Calculate and update missing fields for loans that were disbursed before the fix
-- Date: 2026-01-11
-- Author: System
-- Database: PostgreSQL

-- Step 1: Update all DISBURSED loans that have NULL or 0.00 values
-- This script recalculates: outstandingPrincipal, outstandingInterest, totalOutstandingAmount,
-- weeklyRepaymentAmount, and maturityDate

-- ============================================================================
-- BACKUP FIRST! (Recommended)
-- ============================================================================
-- CREATE TABLE loans_backup_20260111 AS SELECT * FROM loans WHERE loan_status = 'DISBURSED';

-- ============================================================================
-- FIX DISBURSED LOANS WITH MISSING CALCULATIONS (PostgreSQL)
-- ============================================================================

-- Update loans that are DISBURSED but have NULL/0.00 calculated fields
UPDATE loans
SET
    -- Set outstanding principal to disbursed amount (initially full amount)
    outstanding_principal = COALESCE(disbursed_amount, approved_amount),

    -- Calculate interest: (Principal × Rate × Weeks) / (100 × 52)
    outstanding_interest = ROUND(
        CAST((COALESCE(disbursed_amount, approved_amount) * interest_rate * duration_weeks) / 5200.0 AS NUMERIC),
        2
    ),

    -- Calculate total outstanding: Principal + Interest
    total_outstanding_amount = ROUND(
        CAST(COALESCE(disbursed_amount, approved_amount) +
        ((COALESCE(disbursed_amount, approved_amount) * interest_rate * duration_weeks) / 5200.0) AS NUMERIC),
        2
    ),

    -- Calculate weekly repayment: Total / Weeks
    weekly_repayment_amount = ROUND(
        CAST((COALESCE(disbursed_amount, approved_amount) +
        ((COALESCE(disbursed_amount, approved_amount) * interest_rate * duration_weeks) / 5200.0)) /
        NULLIF(duration_weeks, 0) AS NUMERIC),
        2
    ),

    -- Calculate maturity date: disbursement_date + duration_weeks (PostgreSQL syntax)
    maturity_date = CASE
        WHEN disbursement_date IS NOT NULL THEN
            disbursement_date + (duration_weeks || ' weeks')::INTERVAL
        ELSE
            CURRENT_DATE + (duration_weeks || ' weeks')::INTERVAL
    END,

    -- Set audit fields if missing
    created_by = COALESCE(created_by, 'SYSTEM_MIGRATION'),
    updated_by = 'SYSTEM_MIGRATION',
    updated_at = CURRENT_TIMESTAMP

WHERE
    loan_status IN ('DISBURSED', 'ACTIVE')
    AND (
        outstanding_principal IS NULL
        OR outstanding_principal = 0.00
        OR outstanding_interest IS NULL
        OR outstanding_interest = 0.00
        OR total_outstanding_amount IS NULL
        OR total_outstanding_amount = 0.00
        OR weekly_repayment_amount IS NULL
        OR maturity_date IS NULL
    );

-- ============================================================================
-- VERIFICATION QUERY
-- ============================================================================
-- Run this to verify the update worked:
/*
SELECT
    loan_number,
    loan_status,
    disbursed_amount,
    outstanding_principal,
    outstanding_interest,
    total_outstanding_amount,
    weekly_repayment_amount,
    maturity_date,
    disbursement_date,
    updated_by,
    updated_at
FROM loans
WHERE loan_status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC;
*/

-- ============================================================================
-- EXPECTED RESULTS
-- ============================================================================
-- All DISBURSED/ACTIVE loans should now have:
-- ✅ outstanding_principal = disbursed_amount
-- ✅ outstanding_interest = calculated value
-- ✅ total_outstanding_amount = principal + interest
-- ✅ weekly_repayment_amount = calculated value
-- ✅ maturity_date = disbursement_date + duration_weeks
-- ✅ updated_by = 'SYSTEM_MIGRATION'

-- ============================================================================
-- ROLLBACK (if needed)
-- ============================================================================
-- If something goes wrong, restore from backup:
/*
DELETE FROM loans WHERE loan_status IN ('DISBURSED', 'ACTIVE');
INSERT INTO loans SELECT * FROM loans_backup_20260111;
*/

