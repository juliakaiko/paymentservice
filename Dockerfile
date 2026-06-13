# ---------- BUILD ----------
FROM maven:3.9.6-eclipse-temurin-21 AS build

ARG GITHUB_USERNAME
ARG GITHUB_TOKEN

ENV GITHUB_USERNAME=${GITHUB_USERNAME}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

WORKDIR /build

RUN mkdir -p /root/.m2

COPY maven-settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests

# ---------- RUNTIME ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]