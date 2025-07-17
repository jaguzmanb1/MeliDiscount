# ---- Build stage ----
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar pom.xml para cachear dependencias
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiar el resto y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar el JAR compilado
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
