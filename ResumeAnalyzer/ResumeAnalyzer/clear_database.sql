-- ============================================================
-- Resume Analyzer - Clear Database Script
-- ============================================================
-- This script will delete all evaluation records from the database
-- Run this before your demo to start with a clean slate
-- ============================================================

-- Connect to your database first:
-- psql -U postgres -d demodatabase

-- Delete all records from the evaluation table
DELETE FROM evaluation;

-- Reset the ID sequence to start from 1
ALTER SEQUENCE evaluation_id_seq RESTART WITH 1;

-- Verify deletion
SELECT COUNT(*) as total_records FROM evaluation;

-- You should see: total_records = 0

-- ============================================================
-- DONE! Database is now clean and ready for demo
-- ============================================================