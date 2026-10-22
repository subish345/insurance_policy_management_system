import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

/**
 * ============================================================================
 * Insurance Agency Management System - Full Stack Core Java OOP Backend
 * ============================================================================
 * 3-Tier Architecture:
 *   1. OOP Models (Customer, Policy) with Encapsulation & Calculation Logic
 *   2. Data Access Object (InsuranceDAO) with JDBC Connection & SQL Operations
 *   3. HTTP Server Controller (REST API & Web UI Static File Server)
 * ============================================================================
 */

// ============================================================================
// 1. OOP MODEL: POLICY CLASS & UNDERWRITING CALCULATION
// ============================================================================
class Policy {
    private int policyId;
    private int customerId;
    private String policyType; // HEALTH, LIFE, VEHICLE, HOME
    private double coverageAmount;
    private double premium;
    private String createdAt;

    // Constructor for creating a brand-new policy (calculates premium from customer age)
    public Policy(int customerId, String policyType, double coverageAmount, int customerAge) {
        this.customerId = customerId;
        this.policyType = policyType.toUpperCase().trim();
        this.coverageAmount = coverageAmount;
        this.premium = calculatePremium(customerAge);
    }

    // Constructor for loading an existing policy from database
    public Policy(int policyId, int customerId, String policyType, double coverageAmount, double premium, String createdAt) {
        this.policyId = policyId;
        this.customerId = customerId;
        this.policyType = policyType.toUpperCase().trim();
        this.coverageAmount = coverageAmount;
        this.premium = premium;
        this.createdAt = createdAt != null ? createdAt : "";
    }

    /**
     * Core OOP Business Logic:
     * Calculates the annual premium based on Policy Type base rate and Customer Age multiplier.
     * Formula: Annual Premium = (Coverage Amount * Base Rate) * Age Multiplier
     */
    public final double calculatePremium(int age) {
        double baseRate = getBaseRate(this.policyType);
        double ageMultiplier = getAgeMultiplier(age);
        // Round to 2 decimal places
        return Math.round((this.coverageAmount * baseRate * ageMultiplier) * 100.0) / 100.0;
    }

    public static double getBaseRate(String type) {
        if (type == null) return 0.025;
        switch (type.toUpperCase().trim()) {
            case "HEALTH":  return 0.030; // 3.0%
            case "LIFE":    return 0.020; // 2.0%
            case "VEHICLE": return 0.040; // 4.0%
            case "HOME":    return 0.015; // 1.5%
            default:        return 0.025; // 2.5% standard
        }
    }

    public static double getAgeMultiplier(int age) {
        if (age < 25) {
            return 1.10; // Young risk bracket
        } else if (age <= 40) {
            return 1.00; // Standard prime bracket
        } else if (age <= 60) {
            return 1.35; // Mid-age risk adjustment
        } else {
            return 1.75; // Senior risk adjustment
        }
    }

    public static String getAgeBracketName(int age) {
        if (age < 25) return "Under 25 yrs (1.10x risk)";
        if (age <= 40) return "25-40 yrs (1.00x standard)";
        if (age <= 60) return "41-60 yrs (1.35x mid-age)";
        return "Above 60 yrs (1.75x senior)";
    }

    // Getters
    public int getPolicyId() { return policyId; }
    public int getCustomerId() { return customerId; }
    public String getPolicyType() { return policyType; }
    public double getCoverageAmount() { return coverageAmount; }
    public double getPremium() { return premium; }
    public String getCreatedAt() { return createdAt; }

    public String toJson() {
        return String.format(Locale.US,
            "{\"policyId\":%d,\"customerId\":%d,\"policyType\":\"%s\",\"coverageAmount\":%.2f,\"premium\":%.2f,\"baseRate\":%.4f,\"createdAt\":\"%s\"}",
            policyId, customerId, escapeJson(policyType), coverageAmount, premium, getBaseRate(policyType), escapeJson(createdAt)
        );
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}

// ============================================================================
// 2. OOP MODEL: CUSTOMER CLASS (1-to-Many Relationship)
// ============================================================================
class Customer {
    private int customerId;
    private String fullName;
    private int age;
    private String phone;
    private String createdAt;
    private List<Policy> policies; // One customer can hold multiple Policy objects

    public Customer(int customerId, String fullName, int age, String phone) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.age = age;
        this.phone = phone;
        this.createdAt = "";
        this.policies = new ArrayList<>();
    }

    public Customer(int customerId, String fullName, int age, String phone, String createdAt) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.age = age;
        this.phone = phone;
        this.createdAt = createdAt != null ? createdAt : "";
        this.policies = new ArrayList<>();
    }

    public void addPolicy(Policy policy) {
        this.policies.add(policy);
    }

    public double getTotalCoverage() {
        double sum = 0;
        for (Policy p : policies) sum += p.getCoverageAmount();
        return sum;
    }

    public double getTotalPremium() {
        double sum = 0;
        for (Policy p : policies) sum += p.getPremium();
        return sum;
    }

    public int getCustomerId() { return customerId; }
    public String getFullName() { return fullName; }
    public int getAge() { return age; }
    public String getPhone() { return phone; }
    public String getCreatedAt() { return createdAt; }
    public List<Policy> getPolicies() { return policies; }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < policies.size(); i++) {
            sb.append(policies.get(i).toJson());
            if (i < policies.size() - 1) sb.append(",");
        }
        sb.append("]");

        return String.format(Locale.US,
            "{\"customerId\":%d,\"fullName\":\"%s\",\"age\":%d,\"ageBracket\":\"%s\",\"phone\":\"%s\",\"createdAt\":\"%s\",\"policies\":%s,\"totalCoverage\":%.2f,\"totalPremium\":%.2f}",
            customerId, escapeJson(fullName), age, escapeJson(Policy.getAgeBracketName(age)),
            escapeJson(phone), escapeJson(createdAt), sb.toString(), getTotalCoverage(), getTotalPremium()
        );
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}

// ============================================================================
// 3. DATA ACCESS OBJECT (JDBC MYSQL / MARIADB LAYER)
// ============================================================================
class InsuranceDAO {
    private String url;
    private String user;
    private String pass;

    public InsuranceDAO() {
        // Allow environment variables or system properties for connection flexibility
        String propUrl = System.getProperty("DB_URL");
        String envUrl = System.getenv("DB_URL");
        this.url = (propUrl != null && !propUrl.trim().isEmpty()) ? propUrl :
                   (envUrl != null && !envUrl.trim().isEmpty()) ? envUrl :
                   "jdbc:mysql://localhost:3306/insurance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        String propUser = System.getProperty("DB_USER");
        String envUser = System.getenv("DB_USER");
        this.user = (propUser != null && !propUser.trim().isEmpty()) ? propUser :
                    (envUser != null && !envUser.trim().isEmpty()) ? envUser : "root";

        String propPass = System.getProperty("DB_PASS");
        String envPass = System.getenv("DB_PASS");
        this.pass = (propPass != null) ? propPass :
                    (envPass != null) ? envPass : "root";

        // Test connection and auto-initialize tables with retry for container environments
        initDatabase();
    }

    private Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(url, user, pass);
        } catch (SQLException ex) {
            // Graceful fallback attempt for common dev setups if primary password fails
            if ("root".equals(pass)) {
                try {
                    return DriverManager.getConnection(url, user, "password");
                } catch (SQLException ignore) {
                    try {
                        return DriverManager.getConnection(url, user, "");
                    } catch (SQLException ignore2) {}
                }
            }
            throw ex;
        }
    }

    // Auto-create database schema if not present (with retry loop for container environments)
    public void initDatabase() {
        int maxRetries = 15;
        int delaySeconds = 2;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS customers (" +
                    "  customer_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "  full_name VARCHAR(100) NOT NULL," +
                    "  age INT NOT NULL," +
                    "  phone VARCHAR(20) NOT NULL," +
                    "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")"
                );

                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS policies (" +
                    "  policy_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "  customer_id INT NOT NULL," +
                    "  policy_type VARCHAR(30) NOT NULL," +
                    "  coverage_amount DOUBLE NOT NULL," +
                    "  calculated_premium DOUBLE NOT NULL," +
                    "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE" +
                    ")"
                );
                System.out.println("InsuranceDAO: Database connected and verified successfully at " + url);
                return;
            } catch (SQLException e) {
                if (attempt < maxRetries) {
                    System.out.println("InsuranceDAO: Waiting for database (" + attempt + "/" + maxRetries + "): " + e.getMessage());
                    try { Thread.sleep(delaySeconds * 1000L); } catch (InterruptedException ignore) {}
                } else {
                    System.err.println("InsuranceDAO Init Warning: Could not connect after " + maxRetries + " attempts: " + e.getMessage());
                }
            }
        }
    }

    // Add a new Customer to DB
    public Customer createCustomer(String fullName, int age, String phone) throws SQLException {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty.");
        }
        if (age < 18 || age > 110) {
            throw new IllegalArgumentException("Customer age must be between 18 and 110.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required.");
        }

        String sql = "INSERT INTO customers (full_name, age, phone) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, fullName.trim());
            stmt.setInt(2, age);
            stmt.setString(3, phone.trim());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Customer(keys.getInt(1), fullName.trim(), age, phone.trim());
                }
            }
        }
        throw new SQLException("Failed to create customer.");
    }

    // Fetch a single Customer by ID with all their policies
    public Customer getCustomerById(int customerId) throws SQLException {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Customer customer = new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("full_name"),
                        rs.getInt("age"),
                        rs.getString("phone"),
                        getSafeString(rs, "created_at")
                    );
                    loadCustomerPolicies(conn, customer);
                    return customer;
                }
            }
        }
        return null;
    }

    // Delete a customer and cascading policies
    public boolean deleteCustomer(int customerId) throws SQLException {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            return stmt.executeUpdate() > 0;
        }
    }

    // Add a Policy for a Customer (calculates premium using Customer's age)
    public Policy addPolicyToCustomer(int customerId, String policyType, double coverageAmount) throws SQLException {
        Customer customer = getCustomerById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Customer ID #" + customerId + " not found.");
        }
        if (coverageAmount <= 0) {
            throw new IllegalArgumentException("Coverage amount must be greater than zero.");
        }

        // Instantiate Policy object -> automatically calculates premium from customer's age
        Policy newPolicy = new Policy(customerId, policyType, coverageAmount, customer.getAge());

        String sql = "INSERT INTO policies (customer_id, policy_type, coverage_amount, calculated_premium) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, newPolicy.getCustomerId());
            stmt.setString(2, newPolicy.getPolicyType());
            stmt.setDouble(3, newPolicy.getCoverageAmount());
            stmt.setDouble(4, newPolicy.getPremium());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Policy(
                        keys.getInt(1),
                        customerId,
                        newPolicy.getPolicyType(),
                        newPolicy.getCoverageAmount(),
                        newPolicy.getPremium(),
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
                    );
                }
            }
        }
        throw new SQLException("Failed to save policy.");
    }

    // Delete a policy
    public boolean deletePolicy(int policyId) throws SQLException {
        String sql = "DELETE FROM policies WHERE policy_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, policyId);
            return stmt.executeUpdate() > 0;
        }
    }

    // List all Customers with their Policies
    public List<Customer> getAllCustomers() throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY customer_id DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Customer c = new Customer(
                    rs.getInt("customer_id"),
                    rs.getString("full_name"),
                    rs.getInt("age"),
                    rs.getString("phone"),
                    getSafeString(rs, "created_at")
                );
                loadCustomerPolicies(conn, c);
                list.add(c);
            }
        }
        return list;
    }

    private void loadCustomerPolicies(Connection conn, Customer customer) throws SQLException {
        String sql = "SELECT * FROM policies WHERE customer_id = ? ORDER BY policy_id DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customer.getCustomerId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    customer.addPolicy(new Policy(
                        rs.getInt("policy_id"),
                        rs.getInt("customer_id"),
                        rs.getString("policy_type"),
                        rs.getDouble("coverage_amount"),
                        rs.getDouble("calculated_premium"),
                        getSafeString(rs, "created_at")
                    ));
                }
            }
        }
    }

    private static String getSafeString(ResultSet rs, String colName) {
        try {
            String val = rs.getString(colName);
            return val != null ? val : "";
        } catch (SQLException ignore) {
            return "";
        }
    }

    // Aggregated Agency Statistics
    public Map<String, Object> getStats() throws SQLException {
        Map<String, Object> stats = new LinkedHashMap<>();
        int customerCount = 0;
        int policyCount = 0;
        double totalCoverage = 0;
        double totalPremium = 0;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
                if (rs.next()) customerCount = rs.getInt(1);
            }

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*), COALESCE(SUM(coverage_amount), 0), COALESCE(SUM(calculated_premium), 0) FROM policies")) {
                if (rs.next()) {
                    policyCount = rs.getInt(1);
                    totalCoverage = rs.getDouble(2);
                    totalPremium = rs.getDouble(3);
                }
            }
        }

        stats.put("customerCount", customerCount);
        stats.put("policyCount", policyCount);
        stats.put("totalCoverage", totalCoverage);
        stats.put("totalPremium", totalPremium);
        return stats;
    }
}

// ============================================================================
// 4. HTTP SERVER CONTROLLER (REST API + STATIC DASHBOARD)
// ============================================================================
public class InsuranceBackend {
    private static final InsuranceDAO dao = new InsuranceDAO();
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Endpoint: Static Web Files (Dashboard UI, CSS, JS, etc.)
        server.createContext("/", new StaticFileHandler());

        // Endpoint: Customers REST API (GET, POST, DELETE)
        server.createContext("/api/customers", new CustomerHandler());

        // Endpoint: Policies REST API (POST, DELETE)
        server.createContext("/api/policies", new PolicyHandler());

        // Endpoint: Aggregated Analytics & Stats
        server.createContext("/api/stats", new StatsHandler());

        server.setExecutor(null); // default executor
        System.out.println("==================================================================");
        System.out.println(" Insurance Agency Management Server Started Successfully!");
        System.out.println(" Access Dashboard at: http://localhost:" + PORT);
        System.out.println(" REST API Base:      http://localhost:" + PORT + "/api");
        System.out.println("==================================================================");
        server.start();
    }

    /**
     * Serves static frontend files (index.html, style.css, app.js, etc.)
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange, 204);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Clean path to prevent directory traversal
            path = path.replace("..", "").replace("//", "/");
            if (path.startsWith("/")) path = path.substring(1);

            File file = new File(path);
            if (!file.exists() || file.isDirectory()) {
                // Default fallback to index.html for Single Page Applications
                file = new File("index.html");
            }

            if (!file.exists()) {
                String notFound = "<h1>404 Not Found</h1><p>index.html not found in working directory.</p>";
                sendResponse(exchange, 404, "text/html", notFound.getBytes(StandardCharsets.UTF_8));
                return;
            }

            String mimeType = getMimeType(file.getName());
            byte[] bytes = Files.readAllBytes(file.toPath());
            sendResponse(exchange, 200, mimeType, bytes);
        }
    }

    /**
     * Handler for /api/customers
     */
    static class CustomerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                sendCors(exchange, 204);
                return;
            }

            try {
                if ("GET".equals(method)) {
                    String query = exchange.getRequestURI().getQuery();
                    Map<String, String> qmap = parseQuery(query);
                    if (qmap.containsKey("id")) {
                        int id = Integer.parseInt(qmap.get("id"));
                        Customer c = dao.getCustomerById(id);
                        if (c != null) {
                            sendJson(exchange, 200, c.toJson());
                        } else {
                            sendJson(exchange, 404, "{\"error\":\"Customer not found\"}");
                        }
                    } else {
                        List<Customer> customers = dao.getAllCustomers();
                        StringBuilder json = new StringBuilder("[");
                        for (int i = 0; i < customers.size(); i++) {
                            json.append(customers.get(i).toJson());
                            if (i < customers.size() - 1) json.append(",");
                        }
                        json.append("]");
                        sendJson(exchange, 200, json.toString());
                    }
                } else if ("POST".equals(method)) {
                    Map<String, String> params = parseBody(exchange);
                    String fullName = params.get("fullName");
                    int age = Integer.parseInt(params.get("age"));
                    String phone = params.get("phone");

                    Customer created = dao.createCustomer(fullName, age, phone);
                    sendJson(exchange, 201, created.toJson());
                } else if ("DELETE".equals(method)) {
                    Map<String, String> qmap = parseQuery(exchange.getRequestURI().getQuery());
                    if (!qmap.containsKey("id")) {
                        sendJson(exchange, 400, "{\"error\":\"Missing id parameter\"}");
                        return;
                    }
                    int id = Integer.parseInt(qmap.get("id"));
                    boolean deleted = dao.deleteCustomer(id);
                    if (deleted) {
                        sendJson(exchange, 200, "{\"message\":\"Customer and policies deleted successfully\",\"customerId\":" + id + "}");
                    } else {
                        sendJson(exchange, 404, "{\"error\":\"Customer not found\"}");
                    }
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for /api/policies
     */
    static class PolicyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                sendCors(exchange, 204);
                return;
            }

            try {
                if ("POST".equals(method)) {
                    Map<String, String> params = parseBody(exchange);
                    int customerId = Integer.parseInt(params.get("customerId"));
                    String policyType = params.get("policyType");
                    double coverageAmount = Double.parseDouble(params.get("coverageAmount"));

                    Policy created = dao.addPolicyToCustomer(customerId, policyType, coverageAmount);
                    sendJson(exchange, 201, created.toJson());
                } else if ("DELETE".equals(method)) {
                    Map<String, String> qmap = parseQuery(exchange.getRequestURI().getQuery());
                    if (!qmap.containsKey("id")) {
                        sendJson(exchange, 400, "{\"error\":\"Missing policy id parameter\"}");
                        return;
                    }
                    int id = Integer.parseInt(qmap.get("id"));
                    boolean deleted = dao.deletePolicy(id);
                    if (deleted) {
                        sendJson(exchange, 200, "{\"message\":\"Policy deleted successfully\",\"policyId\":" + id + "}");
                    } else {
                        sendJson(exchange, 404, "{\"error\":\"Policy not found\"}");
                    }
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for /api/stats
     */
    static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange, 204);
                return;
            }

            try {
                Map<String, Object> stats = dao.getStats();
                String json = String.format(Locale.US,
                    "{\"customerCount\":%d,\"policyCount\":%d,\"totalCoverage\":%.2f,\"totalPremium\":%.2f}",
                    stats.get("customerCount"), stats.get("policyCount"),
                    stats.get("totalCoverage"), stats.get("totalPremium")
                );
                sendJson(exchange, 200, json);
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // ------------------------------------------------------------------------
    // Utility Methods for Request Parsing & Responses
    // ------------------------------------------------------------------------

    private static Map<String, String> parseBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        Map<String, String> map = new HashMap<>();

        if (body.isEmpty()) return map;

        // Check if JSON format
        if (body.startsWith("{") && body.endsWith("}")) {
            String inner = body.substring(1, body.length() - 1);
            // Simple robust JSON key-value extraction for flat objects
            String[] tokens = inner.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String token : tokens) {
                String[] kv = token.split(":", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim().replace("\"", "");
                    String v = kv[1].trim().replace("\"", "");
                    map.put(k, v);
                }
            }
            return map;
        }

        // Form URL Encoded format (param1=val1&param2=val2)
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(
                    URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                );
            }
        }
        return map;
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(
                    URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                );
            }
        }
        return map;
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendCors(HttpExchange exchange, int status) throws IOException {
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        addCorsHeaders(exchange);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void sendResponse(HttpExchange exchange, int status, String mimeType, byte[] bytes) throws IOException {
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", mimeType + "; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "text/plain";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
