# Сборка
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY wsdl.xml ./
RUN mvn -q -DskipTests package

# Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/edoc-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 4102
ENV SERVER_PORT=4102
ENTRYPOINT ["java","-jar","/app/app.jar"]
