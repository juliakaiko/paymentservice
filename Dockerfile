FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/paymentservice-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]