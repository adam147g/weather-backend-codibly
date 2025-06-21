FROM maven:3.9.9-eclipse-temurin-17

COPY . .
RUN mvn clean install -DskipTests
CMD ["java", "-jar", "target/weather-backend-0.0.1-SNAPSHOT.jar"]