# Use Java 17
FROM openjdk:17-jdk-slim

# Working directory
WORKDIR /app

# Copy project
COPY . .

# Give permission to mvnw
RUN chmod +x mvnw

# Build the project
RUN ./mvnw clean package -DskipTests

# Expose port
EXPOSE 8080

# Run Spring Boot
CMD ["java","-jar","target/*.jar"]