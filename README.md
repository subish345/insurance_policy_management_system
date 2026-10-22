# Insurance Agency Management System

A beginner-friendly, full-stack 3-tier insurance agency application built with **Core Java (OOP)**, **Built-in JDK `HttpServer`**, **MySQL / MariaDB (JDBC)**, and a **Minimalist HTML/CSS/JavaScript Web Frontend**.

The project is fully containerized with **Docker** and **Podman** so it runs seamlessly on **Windows, Linux, and macOS** with zero manual dependency setup.

> **Note:** The system starts completely fresh with **no dummy data**. You can register real customers and issue policies through the web portal or REST API.

---

## 📋 Table of Contents
1. [Architecture Overview](#1-architecture-overview)
2. [Premium Calculation Formula](#2-premium-calculation-formula)
3. [Database Design (MySQL/MariaDB)](#3-database-design)
4. [Backend Code Structure (Java OOP)](#4-backend-code-structure)
5. [How to Run (Docker, Podman, or Local)](#5-how-to-run)
6. [API Endpoints](#6-api-endpoints)
7. [Comprehensive Documentation (explain.docx)](#7-comprehensive-documentation)

---

## 1. Architecture Overview

This project uses a standard **3-Tier Architecture**:

1. **Presentation Layer (Frontend)**:
   - File: [`index.html`](file:///home/subish-loq/Downloads/insurance_policy_management_system/index.html)
   - Built with clean, minimalist HTML5, Vanilla CSS, and modern JavaScript.
   - Includes real-time calculation preview and 1-to-many customer policy listing.
   
2. **Application Layer (Backend)**:
   - File: [`InsuranceBackend.java`](file:///home/subish-loq/Downloads/insurance_policy_management_system/InsuranceBackend.java)
   - Pure Core Java using the JDK's built-in `HttpServer` (runs on port `8080` without requiring external Tomcat or Spring).
   - OOP design separating business logic (`Policy`, `Customer`), database access (`InsuranceDAO`), and HTTP routing (`InsuranceBackend`).

3. **Data Layer (Database)**:
   - File: [`schema.sql`](file:///home/subish-loq/Downloads/insurance_policy_management_system/schema.sql)
   - MySQL 8.x / MariaDB 11.x relational database (`insurance_db`).
   - Tables: `customers` (parent) and `policies` (child) with `FOREIGN KEY ... ON DELETE CASCADE`.

---

## 2. Premium Calculation Formula

The annual premium is computed dynamically inside the `Policy` class using:

$$\text{Annual Premium} = (\text{Coverage Amount} \times \text{Base Rate}) \times \text{Age Multiplier}$$

### A. Policy Types & Base Rates (% of Coverage)
| Policy Type | Code | Base Rate | Example (₹5,00,000 Coverage) |
| :--- | :--- | :--- | :--- |
| **Health Insurance** | `HEALTH` | **3.0%** (0.030) | ₹15,000 base |
| **Life Insurance** | `LIFE` | **2.0%** (0.020) | ₹10,000 base |
| **Vehicle Insurance** | `VEHICLE` | **4.0%** (0.040) | ₹20,000 base |
| **Home Insurance** | `HOME` | **1.5%** (0.015) | ₹7,500 base |

### B. Customer Age Risk Brackets & Multipliers
| Customer Age Bracket | Multiplier | Underwriting Risk Class |
| :--- | :--- | :--- |
| **Under 25 years** | **1.10x** | Young driver / novice risk loading |
| **25 to 40 years** | **1.00x** | Standard baseline |
| **41 to 60 years** | **1.35x** | Mid-age risk adjustment |
| **Above 60 years** | **1.75x** | Senior risk adjustment |

#### Example:
A 45-year-old customer (`Age Multiplier = 1.35x`) takes a **Health Policy** with **₹5,00,000** coverage:
$$\text{Premium} = (5,00,000 \times 0.030) \times 1.35 = 15,000 \times 1.35 = \mathbf{₹20,250.00 \text{ / year}}$$

---

## 3. Database Design

```sql
-- Parent Table: Customers
CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Child Table: Policies (One-to-Many Relationship)
CREATE TABLE IF NOT EXISTS policies (
    policy_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    policy_type VARCHAR(30) NOT NULL,
    coverage_amount DOUBLE NOT NULL,
    calculated_premium DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);
```

---

## 4. Backend Code Structure

The backend is organized in [`InsuranceBackend.java`](file:///home/subish-loq/Downloads/insurance_policy_management_system/InsuranceBackend.java):

- **`Policy` Class**: Encapsulates policy ID, customer ID, type, coverage, and the core OOP method `calculatePremium(int age)`.
- **`Customer` Class**: Encapsulates customer attributes and holds a `List<Policy>` representing the 1-to-many relationship.
- **`InsuranceDAO` Class**: Manages database operations via JDBC (`createCustomer`, `getCustomerById`, `getAllCustomers`, `addPolicyToCustomer`, `deletePolicy`, `deleteCustomer`). Includes connection retries for containers.
- **`InsuranceBackend` Server**: Sets up the HTTP server on port `8080`, handles CORS, serves `index.html`, and handles JSON/form REST requests.

---

## 5. How to Run

### 🐳 Option 1: Run with Docker or Podman (Recommended for Any OS)
No need to install Java or MySQL on your host machine.

#### Windows:
- Double-click **`docker-start.bat`**, OR run:
  ```powershell
  docker compose up -d --build
  ```

#### Linux & macOS:
- Run the launch script:
  ```bash
  ./docker-start.sh
  ```
- Or run manually:
  ```bash
  docker compose up -d --build
  # OR if using Podman:
  podman compose up -d --build
  ```

#### Open in Browser:
👉 **`http://localhost:8080`**

#### Stop Containers:
```bash
docker compose down    # or: podman compose down
```

---

### 💻 Option 2: Run Natively on Host Machine
Requires **Java 17+** and a running **MySQL / MariaDB** server.

```bash
# 1. Start your local MySQL/MariaDB service
sudo systemctl start mariadb   # or: sudo systemctl start mysql

# 2. Compile Java backend
javac -cp .:mysql-connector-j.jar InsuranceBackend.java

# 3. Run backend
java -cp .:mysql-connector-j.jar -DDB_USER=root -DDB_PASS=root InsuranceBackend
```

---

## 6. API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Serves the web dashboard (`index.html`) |
| `GET` | `/api/customers` | Lists all customers and their policies |
| `GET` | `/api/customers?id={id}` | Gets a specific customer by ID |
| `POST` | `/api/customers` | Registers a new customer (`fullName`, `age`, `phone`) |
| `DELETE` | `/api/customers?id={id}` | Deletes a customer and all their policies |
| `POST` | `/api/policies` | Issues a policy and calculates premium from age |
| `DELETE` | `/api/policies?id={id}` | Deletes a specific policy |
| `GET` | `/api/stats` | Returns system summary statistics |

---

## 7. Comprehensive Documentation (`explain.docx`)

A complete, formatted Microsoft Word document is generated in the root folder:
📄 **[`explain.docx`](file:///home/subish-loq/Downloads/insurance_policy_management_system/explain.docx)**

This document explains:
- Every class, method, and variable in `InsuranceBackend.java`.
- How the database relational schema and foreign keys work.
- The mathematical logic behind underwriting risk calculations.
- The minimalist design principles of `index.html`.
- How Docker and Podman containerize and network the application.
- Step-by-step beginner guide to modifying and extending the system.
