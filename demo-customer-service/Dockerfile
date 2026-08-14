FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY target/demo-customer-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app.jar"]
