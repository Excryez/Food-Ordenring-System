FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

COPY pemesanan/pom.xml ./pom.xml
COPY pemesanan/src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
