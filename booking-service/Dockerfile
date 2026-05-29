FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY course-service/pom.xml course-service/pom.xml
COPY booking-service/pom.xml booking-service/pom.xml
COPY booking-service/src booking-service/src
RUN mvn -pl booking-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/booking-service/target/booking-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
