# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the jar without running tests
RUN mvn -B clean package -DskipTests

# Stage 2: Run the jar with a lightweight JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
# Render expects the container to listen on the port indicated by $PORT (exposed as 8080 here)
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
