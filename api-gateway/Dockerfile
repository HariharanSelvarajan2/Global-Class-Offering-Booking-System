FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY course-service/pom.xml course-service/pom.xml
COPY booking-service/pom.xml booking-service/pom.xml
COPY api-gateway/src api-gateway/src
RUN mvn -pl api-gateway -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/api-gateway-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
