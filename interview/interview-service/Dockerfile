FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/interview-service-*.jar app.jar

EXPOSE 9083

ENTRYPOINT ["java", "-jar", "app.jar"]
