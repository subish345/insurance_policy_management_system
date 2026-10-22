# ==============================================================================
# Multi-stage Dockerfile for OmniShield Insurance Agency Management System
# Compatible with Docker, Podman, and Kubernetes on Linux, macOS, and Windows
# ==============================================================================

# Stage 1: Build & Compile
FROM docker.io/library/eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy source code and JDBC driver
COPY InsuranceBackend.java .
COPY mysql-connector-j.jar .

# Compile the core Java OOP application
RUN javac -cp .:mysql-connector-j.jar InsuranceBackend.java

# Stage 2: Minimal Lightweight Runtime
FROM docker.io/library/eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled classes, driver, and frontend assets from builder
COPY --from=builder /build/*.class ./
COPY --from=builder /build/mysql-connector-j.jar ./
COPY index.html ./

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose default HTTP server port
EXPOSE 8080

# Configure Environment Variables (overridable at container runtime)
ENV PORT=8080 \
    DB_URL="jdbc:mysql://db:3306/insurance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
    DB_USER="root" \
    DB_PASS="root"

# Run the backend server
CMD ["java", "-cp", ".:mysql-connector-j.jar", "InsuranceBackend"]
