# Build stage
FROM maven:3.9-eclipse-temurin-26 AS builder

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:26-jdk

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]