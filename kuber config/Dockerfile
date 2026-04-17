# -------- Stage 1: Build --------
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests clean package && ls -lah /app/target

# -------- Stage 2: Runtime --------
FROM eclipse-temurin:17-jre

ENV APP_HOME=/opt/anubis-edoc \
    SERVER_PORT=4102

WORKDIR ${APP_HOME}

COPY --from=builder /app/target/*.jar /opt/anubis-edoc/anubis-edoc.jar

EXPOSE 4102

ENTRYPOINT ["java", "-jar", "/opt/anubis-edoc/anubis-edoc.jar"]