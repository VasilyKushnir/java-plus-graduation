# =========================
# Build stage
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy only pom files first to leverage Docker cache
COPY pom.xml .
COPY */pom.xml ./
COPY */*/pom.xml ./

# Copy full source
COPY . .

# Build all modules
RUN mvn -B -T 1C clean package -DskipTests

# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the final jar (adjust path if needed)
COPY --from=builder /build/stats-service/stats-server/target/*jar app.jar


ENTRYPOINT ["java","-jar","app.jar"]