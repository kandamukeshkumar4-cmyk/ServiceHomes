FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk add --no-cache postgresql-client wget
RUN addgroup -S app && adduser -S -G app app

WORKDIR /opt/servicehomes

COPY --from=build /workspace/target/*.jar app.jar

RUN chown -R app:app /opt/servicehomes

USER app

EXPOSE 8080 8081

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
