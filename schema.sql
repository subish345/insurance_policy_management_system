-- ==============================================================================
-- Insurance Agency Management System - Database Schema
-- Compatible with MySQL 8.x and MariaDB 10.x / 11.x
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS insurance_db;
USE insurance_db;

-- 1. Customers Table (Parent Table)
-- Stores customer personal details.
CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Policies Table (Child Table)
-- One customer can hold many policies (1-to-Many Relationship).
-- FOREIGN KEY links each policy to a customer in the customers table.
-- ON DELETE CASCADE ensures if a customer is deleted, their policies are removed automatically.
CREATE TABLE IF NOT EXISTS policies (
    policy_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    policy_type VARCHAR(30) NOT NULL,
    coverage_amount DOUBLE NOT NULL,
    calculated_premium DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);
