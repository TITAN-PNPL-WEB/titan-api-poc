FROM eclipse-temurin:22-jre

WORKDIR /app

COPY target/titan-api-poc-1.0-SNAPSHOT.jar app.jar
COPY plugins/ plugins/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]