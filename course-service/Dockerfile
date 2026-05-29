FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY course-service/pom.xml course-service/pom.xml
COPY booking-service/pom.xml booking-service/pom.xml
COPY course-service/src course-service/src
RUN mvn -pl course-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/course-service/target/course-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
